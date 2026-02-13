package comp370.srms;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class ServerMonitor extends SrmsNode {
    private static final int DEFAULT_MONITOR_PORT = 3000;

    private final AtomicInteger nextServerId = new AtomicInteger(1);
    private final AtomicReference<String> primaryServerId = new AtomicReference<>(null);
    private final Map<String, ServerRecord> servers = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    private ServerMonitor() {
        super("MONITOR");
    }

    public static void main(String[] args) {
        ServerMonitor monitor = new ServerMonitor();

        int port;
        try {
            port = monitor.parsePort(args.length > 0 ? args[0] : String.valueOf(DEFAULT_MONITOR_PORT));
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        monitor.start(port);
    }

    private void start(int port) {
        pool.submit(() -> startListener(port));
        pool.submit(() -> startClientListener(port+1));
    }

    private void startListener(int port) {
        try (ServerSocket monitorSocket = new ServerSocket(port)) {
            log("Monitor listening on port " + port);
            attachShutdownHook(() -> shutdownMonitor(monitorSocket));

            while (isRunning() && !monitorSocket.isClosed()) {
                try {
                    Socket connection = monitorSocket.accept();
                    pool.submit(() -> handleServerConnection(connection));
                } catch (IOException e) {
                    if (!monitorSocket.isClosed()) {
                        log("Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log("Monitor failed to start: " + e.getMessage());
            System.exit(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private void startClientListener(int port) {
        try (ServerSocket monitorSocket = new ServerSocket(port)) {
            log("Client monitor listening on " + port);
            attachShutdownHook(() -> shutdownMonitor(monitorSocket));

            while (isRunning() && !monitorSocket.isClosed()) {
                try {
                    Socket connection = monitorSocket.accept();
                    pool.submit(() -> handleClientConnection(connection));
                } catch (IOException e) {
                    if (!monitorSocket.isClosed()) {
                        log("Accept error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log("Client monitor failed to start: " + e.getMessage());
            System.exit(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private void handleClientConnection(Socket socket) {
        String remote = String.valueOf(socket.getRemoteSocketAddress());
        log("Client connected from " + remote);
        try (MessageSocket msgSocket = MessageSocket.fromSocket(socket)) {
            processIncomingClientMessage(msgSocket);
        } catch (IOException e) {
            log("Connection error (" + remote + "): " + e.getMessage());
        } finally {
            log("Client disconnected: " + remote);
        }
    }

    private void shutdownMonitor(ServerSocket monitorSocket) {
        try {
            monitorSocket.close();
        } catch (IOException ignored) {
            // Ignore exception on shutdown path.
        }
        pool.shutdownNow();
        log("Monitor shutdown complete");
    }

    private void handleServerConnection(Socket socket) {
        String remote = String.valueOf(socket.getRemoteSocketAddress());
        log("Server connected from " + remote);

        String assignedId = null;
        try (MessageSocket messageSocket = MessageSocket.fromSocket(socket)) {
            assignedId = registerServer(messageSocket, remote);
            if (assignedId != null) {
                processIncomingMessages(messageSocket, assignedId);
            }
        } catch (IOException e) {
            log("Connection error (" + remote + "): " + e.getMessage());
        } finally {
            if (assignedId != null) {
                removeServer(assignedId);
            }
            log("Server disconnected: " + remote);
        }
    }

    private String registerServer(MessageSocket messageSocket, String remote) throws IOException {
        MessageSerializer.Message helloMessage = messageSocket.readMessage();
        if (helloMessage == null) {
            return null;
        }
        if (helloMessage.type() != MessageSerializer.Type.HELLO) {
            messageSocket.send(MessageSerializer.serializeError("Expected HELLO as first message"));
            return null;
        }

        MessageSerializer.Message portMessage = messageSocket.readMessage();
        if (portMessage == null) {
            return null;
        }
        if (portMessage.type() != MessageSerializer.Type.PORT) {
            messageSocket.send(MessageSerializer.serializeError("Expected PORT as second message, got " + portMessage.type()));
            return null;
        }
        String portString = portMessage.detail();
        int port = Integer.parseInt(portString);

        String assignedId = assignServerId();
        ServerRole assignedRole = assignRole(assignedId);
        servers.put(assignedId, new ServerRecord(assignedRole, System.currentTimeMillis(), remote, messageSocket, port));
        messageSocket.send(MessageSerializer.serializeAssign(assignedId, assignedRole));
        announceServerUpdate(
                MessageSerializer.Message.assign(assignedId, assignedRole),
                assignedId,
                assignedRole,
                remote);
        return assignedId;
    }

    private void processIncomingMessages(MessageSocket messageSocket, String assignedId) throws IOException {
        MessageSerializer.Message message;
        while ((message = messageSocket.readMessage()) != null) {
            String response = processServerMessage(assignedId, message);
            messageSocket.send(response);
        }
    }

    private void processIncomingClientMessage(MessageSocket msgSocket) throws IOException {
        MessageSerializer.Message message;
        while ((message = msgSocket.readMessage()) != null) {
            String response = processClientMessage(message);
            msgSocket.send(response);
        }
    }

    private String processClientMessage(MessageSerializer.Message message) {
//        log(String.valueOf(message.type()));
        return switch (message.type()) {
            case GETPRIMARY -> {
//                log("getprimary");
//                log(servers.get(primaryServerId.toString()).remoteAddress);
                ServerRecord primaryServer = servers.get(primaryServerId.toString());
                String primaryRemote = primaryServer.remoteAddress;
                String primaryPort = primaryServer.portForClient + "";
                String[] prSplit = primaryRemote.split(":");
                String ipPort = prSplit[0] + ":" + primaryPort;
                log("Got port " + ipPort);
                yield MessageSerializer.serializePrimary(ipPort, primaryServerId.toString());
            }
            case PING -> {
                yield MessageSerializer.serializePing();
            }
            default -> {
                log("Unsupported message type from client: " + message.type());
                yield MessageSerializer.serializeError("Unsupported message type: " + message.type());
            }
        };
    }

    private String processServerMessage(String assignedId, MessageSerializer.Message message) {
        return switch (message.type()) {
            case HEARTBEAT -> {
                if (!assignedId.equals(message.serverId())) {
                    yield MessageSerializer.serializeError("Heartbeat server-id mismatch");
                }

                ServerRecord existing = servers.get(assignedId);
                if (existing != null) {
                    servers.put(assignedId, new ServerRecord(
                            existing.role(),
                            System.currentTimeMillis(),
                            existing.remoteAddress(),
                            existing.messageSocket(),
                            existing.portForClient()));
                }
                log("Heartbeat from " + assignedId + " at " + message.timestampMs());
                yield MessageSerializer.serializeAck();
            }
            case ACK -> {
                log("ACK received from " + assignedId);
                yield MessageSerializer.serializeAck();
            }
            case INVALID -> {
                log("Invalid message from " + assignedId + ": " + message.detail());
                yield MessageSerializer.serializeError(message.detail());
            }
            case ERROR -> {
                log("Server reported error (" + assignedId + "): " + message.detail());
                yield MessageSerializer.serializeAck();
            }
            default -> {
                log("Unsupported message type from " + assignedId + ": " + message.type());
                yield MessageSerializer.serializeError("Unsupported message type: " + message.type());
            }
        };
    }

    private String assignServerId() {
        return "s" + nextServerId.getAndIncrement();
    }

    private ServerRole assignRole(String serverId) {
        if (primaryServerId.compareAndSet(null, serverId)) {
            return ServerRole.PRIMARY;
        }
        return ServerRole.BACKUP;
    }

    private void removeServer(String serverId) {
        ServerRecord removed = servers.remove(serverId);
        if (removed.role() == ServerRole.PRIMARY) {
            primaryServerId.compareAndSet(serverId, null);
            promoteLowestBackupToPrimary();
        }

        announceServerUpdate(
                MessageSerializer.Message.error("DISCONNECT"),
                serverId,
                removed.role(),
                removed.remoteAddress());

        if (removed.role() == ServerRole.PRIMARY) {
            // Handle promotion here (or extract)
        }
    }

    private void promoteLowestBackupToPrimary() {
        ServerRecord candidate = null;
        String candidateId = null;

        for (Map.Entry<String, ServerRecord> entry : servers.entrySet()) {
            String id = entry.getKey();
            ServerRecord record = entry.getValue();
            if (record.role() != ServerRole.BACKUP) continue;
            if(candidate == null || numericId(id) < numericId(candidateId)){
                candidate = record;
                candidateId = id;
            }
        }
        if(candidate == null){
            log("No BACKUP available to promote");
        }
        
        primaryServerId.set(candidateId);
        servers.put(candidateId, new ServerRecord(
                ServerRole.PRIMARY,
                candidate.lastHeartbeatMs(),
                candidate.remoteAddress(),
                candidate.messageSocket(),
                candidate.portForClient()
        ));

        log("Promoting " + candidateId + " to PRIMARY");
        try{
            candidate.messageSocket().send(MessageSerializer.serializePromote(candidateId));
        } catch (IOException e) {
            log("Failed to send PROMOTE to " + candidateId + ": " + e.getMessage());
        }
    }

    private int numericId(String serverId){
        try {
            if (serverId.startsWith("s")) {
                return Integer.parseInt(serverId.substring(1));
        }
        return Integer.parseInt(serverId);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private void announceServerUpdate(
            MessageSerializer.Message message,
            String serverId,
            ServerRole role,
            String remote) {
        String event = message.type() == MessageSerializer.Type.ASSIGN
                ? "SERVER_CONNECTED"
                : "SERVER_DISCONNECTED";

        log(event + " id=" + serverId
                + " role=" + role
                + " remote=" + remote
                + " activeServers=" + servers.size());
    }

    private record ServerRecord(
            ServerRole role,
            long lastHeartbeatMs,
            String remoteAddress,
            MessageSocket messageSocket,
            int portForClient) {
    }
}
