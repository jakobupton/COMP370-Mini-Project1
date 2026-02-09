package comp370.srms;

import java.io.IOException;

public final class ServerProcess extends SrmsNode {
    private static final String DEFAULT_MONITOR_HOST = "localhost";
    private static final int DEFAULT_MONITOR_PORT = 3000;
    private static final int DEFAULT_HEARTBEAT_MS = 1000;
    private static final int RECONNECT_DELAY_MS = 1500;

    private ServerProcess() {
        super("SERVER");
    }

    public static void main(String[] args) {
        ServerProcess process = new ServerProcess();

        Config config;
        try {
            config = process.parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        process.start(config);
    }

    private void start(Config config) {
        attachShutdownHook(() -> log("Shutdown signal received"));

        while (isRunning()) {
            runSession(config);

            if (isRunning()) {
                sleepQuietly(RECONNECT_DELAY_MS);
            }
        }
    }

    private void runSession(Config config) {
        try (MessageSocket messageSocket = MessageSocket.connect(config.monitorHost(), config.monitorPort())) {
            log("Connected to monitor at " + config.monitorHost() + ":" + config.monitorPort());
            messageSocket.send(MessageSerializer.serializeHello());

            Identity identity = readAssignment(messageSocket);
            log("Assigned id=" + identity.serverId() + " role=" + identity.role().toWire());

            heartbeatLoop(messageSocket, identity, config.heartbeatIntervalMs());
        } catch (IOException e) {
            log("Monitor connection lost: " + e.getMessage());
        }
    }

    private void heartbeatLoop(MessageSocket messageSocket, Identity identity, int heartbeatIntervalMs)
            throws IOException {
        while (isRunning() && !messageSocket.isClosed()) {
            messageSocket.send(MessageSerializer.serializeHeartbeat(identity.serverId(), System.currentTimeMillis()));
            readAck(messageSocket, identity.serverId());
            sleepQuietly(heartbeatIntervalMs);
            if (Thread.currentThread().isInterrupted()) {
                requestStop();
            }
        }
    }

    private void readAck(MessageSocket messageSocket, String serverId) throws IOException {
        MessageSerializer.Message message = readRequiredMessage(
                messageSocket,
                "Connection closed while waiting for ACK");
        switch (message.type()) {
            case ACK -> log("Heartbeat acknowledged by monitor for " + serverId);
            case INVALID -> throw new IOException("Invalid monitor message: " + message.detail());
            case ERROR -> throw new IOException("Monitor error: " + message.detail());
            default -> throw new IOException("Expected ACK, got " + message.type());
        }
    }

    private Identity readAssignment(MessageSocket messageSocket) throws IOException {
        MessageSerializer.Message message = readRequiredMessage(
                messageSocket,
                "Connection closed before assignment");
        switch (message.type()) {
            case ASSIGN -> {
                if (message.role() == null) {
                    throw new IOException("ASSIGN missing role");
                }
                return new Identity(message.serverId(), message.role());
            }
            case INVALID -> throw new IOException("Invalid assignment response: " + message.detail());
            case ERROR -> throw new IOException("Monitor error during assignment: " + message.detail());
            default -> throw new IOException("Expected ASSIGN from monitor, got " + message.type());
        }
    }

    private MessageSerializer.Message readRequiredMessage(MessageSocket messageSocket, String closedMessage)
            throws IOException {
        MessageSerializer.Message message = messageSocket.readMessage();
        if (message == null) {
            throw new IOException(closedMessage);
        }
        return message;
    }

    private Config parseArgs(String[] args) {
        if (args.length > 3) {
            throw new IllegalArgumentException("Usage: [monitor-host] [monitor-port] [heartbeat-ms]");
        }

        String monitorHost = args.length > 0 ? args[0].trim() : DEFAULT_MONITOR_HOST;
        int monitorPort = args.length > 1 ? parsePort(args[1]) : DEFAULT_MONITOR_PORT;
        int heartbeatMs = args.length > 2 ? parseHeartbeat(args[2]) : DEFAULT_HEARTBEAT_MS;

        return new Config(monitorHost, monitorPort, heartbeatMs);
    }

    private record Config(
            String monitorHost,
            int monitorPort,
            int heartbeatIntervalMs) {
    }

    private record Identity(String serverId, ServerRole role) {
    }
}
