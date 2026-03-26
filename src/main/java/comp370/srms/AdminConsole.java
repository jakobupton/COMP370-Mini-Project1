package comp370.srms;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminConsole {

    private static final Path MONITOR_LOG = Path.of("logs/MONITOR.log");
    private static String currentPrimaryID = "";
    private static String currentPrimaryAddr = "";
    private static String monitorIp = "127.0.0.1";
    private static int monitorPort = 3001;
    private static ServerSocket observerSocket;
    private static int observerPort;

    private static final Scanner scanner = new Scanner(System.in);
    private static volatile boolean refreshRequested = false;
    private static volatile boolean suppressNextRefresh = false;
    private static MessageSocket Monitor;
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {

        if (!Files.exists(MONITOR_LOG)) {
            System.out.println("No monitor log found. Start the monitor first.");
            return;
        }

        pool.submit(() -> {
            try {
                startObserverListener();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            ConnectToMonitor();
        } catch (Exception e) {
            System.out.println(e);
            scanner.close();
            shutdown();
            return;
        }



        boolean exit = false;

        while (!exit) {
            ShowServerStatus();


            System.out.println("""
            1. View most recent logs
            2. Perform manual failover (Stops current primary)
            3. Reconnect to monitor
            4. Exit
            """);
            System.out.print("> ");

            String selection = null;
            while (selection == null) {
                if (refreshRequested) {
                    ShowServerStatus();
                    refreshRequested = false;

                    // Reprint prompt
                    System.out.println("""
                    1. View most recent logs
                    2. Perform manual failover (Stops current primary)
                    3. Reconnect to monitor
                    4. Exit
                    """);
                    System.out.print("> ");
                }

                try {
                    if (System.in.available() > 0) {
                        selection = scanner.nextLine();
                    } else {
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            switch (selection) {
                case "1":
                    ShowRecentLogs();
                    break;

                case "2":
                    Failover();
                    break;

                case "3":
                    Line();
                    try {
                        ConnectToMonitor();
                    } catch (Exception e) {
                        System.out.println("Unable to connect to monitor:");
                        System.out.println(e);
                    }
                    Line();
                    break;

                case "4":

                    exit = true;
                    break;

                default:
                    System.out.println("Invalid input.");
            }
        }
        shutdown();
        scanner.close();
        return;
    }

    private static void ConnectToMonitor() throws Exception {
        int attempts = 0;
        int maxAttempts = 5;
        String MonitorIP = "127.0.0.1";
        int MonitorPort = 3001;
        Exception e = new Exception();
        while (attempts < maxAttempts) {
            try {
                Monitor = MessageSocket.connect(MonitorIP, MonitorPort);
                Monitor.send(MessageSerializer.serializeRegisterObserver("127.0.0.1:" + observerPort));
                Monitor.readMessage();
                return;
            } catch (Exception ex) {
                e = ex;
                attempts++;
            }
        }
        throw e;
    }

    private static void ShowRecentLogs() {
        Line();
        try {
            List<String> lines = Files.readAllLines(MONITOR_LOG);

            System.out.println("=== RECENT LOGS ===");
            lines.stream()
                    .skip(Math.max(0, lines.size() - 10))
                    .forEach(System.out::println);
        } catch (Exception e) {
            System.out.println("Could not read logs:");
            System.out.println(e.getMessage());
        }
        Line();
    }

    private static void ShowServerStatus() {
        Line();
        System.out.println("=== SERVER HEALTH ===");

        boolean MonitorOK = false;
        try {
            MonitorOK = CheckMonitorConnection();
        } catch (Exception ignored) {}

        boolean PrimaryOK = false;
        if (MonitorOK) {
            System.out.println("Monitor: OK");
            try {
                PrimaryOK = CheckPrimaryConnection();
            } catch (Exception ignored) {}
            if (PrimaryOK) {
                System.out.println("Primary: OK");
            } else {
                currentPrimaryAddr = "Not available";
                currentPrimaryID = "Not available";
                System.out.println("Primary: ERROR");
            }
        } else {
            System.out.println("Monitor: ERROR");
            System.out.println("Primary: Cannot check without Monitor");
        }
        System.out.println("Current primary address: " + currentPrimaryAddr);
        System.out.println("Current primary ID: " + currentPrimaryID);
        Line();
    }

    private static void Failover() {
        Line();

        suppressNextRefresh = true;
        try {
            boolean shutdown = ShutdownPrimary();
            if (shutdown) {
                System.out.println("Shutdown succeeded!");
            } else {
                System.out.println("Error during shutdown. Please verify server status.");
            }
        } catch (IOException e) {
            System.out.println("Error shutting down primary:");
            System.out.println(e.getMessage());
        }
        System.out.println("Allowing monitor time to promote a new primary...");
        try {
            Thread.sleep(1200); // Slightly hacky fix to make sure the monitor has time to promote a new primary before status is displayed
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Done!");
        Line();
    }

    private static boolean CheckMonitorConnection() throws IOException {
        Monitor.send(MessageSerializer.serializePing());
        MessageSerializer.Message PingMessage = Monitor.readMessage();
        if (PingMessage == null) {
            return false;
        }
        if (PingMessage.type() != MessageSerializer.Type.PING) {
            return false;
        }
        return true;
    }

    private static boolean CheckPrimaryConnection() throws IOException {
        Monitor.send(MessageSerializer.serializeGetPrimary());
        MessageSerializer.Message PrimaryMessage = Monitor.readMessage();
//        System.out.println(PrimaryMessage.toString());
        if (PrimaryMessage == null) {
            return false;
        }
        if (PrimaryMessage.type() != MessageSerializer.Type.PRIMARY) {
            return false;
        }
        String PrimaryAddr = PrimaryMessage.detail();
        String[] parts = PrimaryAddr.split(":");
        String PrimaryIP = parts[0].replace("/", "");
        int PrimaryPort = Integer.parseInt(parts[1]);
        currentPrimaryAddr = PrimaryIP + ":" + PrimaryPort;
        currentPrimaryID = PrimaryMessage.serverId();
        try (MessageSocket Primary = MessageSocket.connect(PrimaryIP, PrimaryPort)) {
            Primary.send(MessageSerializer.serializeProcess());
            MessageSerializer.Message ProcessingMessage = Primary.readMessage();
            if (ProcessingMessage == null) {
                return false;
            }
            if (ProcessingMessage.type() != MessageSerializer.Type.PROCESSING) {
                return false;
            }
        }
        return true;
    }

    private static boolean ShutdownPrimary() throws IOException {
        Monitor.send(MessageSerializer.serializeGetPrimary());
        MessageSerializer.Message PrimaryMessage = Monitor.readMessage();
        if (PrimaryMessage == null) {
            return false;
        }
        if (PrimaryMessage.type() != MessageSerializer.Type.PRIMARY) {
            return false;
        }
        String PrimaryAddr = PrimaryMessage.detail();
        String[] parts = PrimaryAddr.split(":");
        String PrimaryIP = parts[0].replace("/", "");
        int PrimaryPort = Integer.parseInt(parts[1]);
        try (MessageSocket Primary = MessageSocket.connect(PrimaryIP, PrimaryPort)) {
            Primary.send(MessageSerializer.serializeStop());
            MessageSerializer.Message ProcessingMessage = Primary.readMessage();
            if (ProcessingMessage == null) {
                return false;
            }
            if (ProcessingMessage.type() != MessageSerializer.Type.STOP) {
                return false;
            }
        }
        return true;
    }

    private static void Line() {
        System.out.println("================================");
    }

    private static String Prompt(String message, List<String> acceptableValues) {
        String selection = "";
        boolean ok = false;

        while (!ok) {
            System.out.println(message);
            System.out.print("> ");

            selection = scanner.nextLine();

            if (CheckInput(selection, acceptableValues)) {
                ok = true;
            } else {
                System.out.println("Invalid input. Please try again.");
            }
        }
        return selection;
    }

    private static boolean CheckInput(String input, List<String> acceptableValues) {
        System.out.print(input);
        return acceptableValues.contains(input);
    }

    private static void startObserverListener() throws IOException {
        observerSocket = new ServerSocket(0);

        System.out.println("Opened observer listener on port " + observerSocket.getLocalPort());
        observerPort = observerSocket.getLocalPort();

        while (!observerSocket.isClosed()) {
            try {
                Socket connection = observerSocket.accept();
                pool.submit(() -> handleObserverConnection(connection));
            } catch (IOException e) {
                if (observerSocket.isClosed()) break; // 👈 exit cleanly
            }
        }
    }

    private static void handleObserverConnection(Socket socket) {
        System.out.println("Monitor connected to observer.");
        try (MessageSocket msgSocket = MessageSocket.fromSocket(socket)) {
            processIncomingUpdate(msgSocket);
        } catch (IOException ignored) {
//            System.out.println("Observer error!");
        }
    }

    private static void processIncomingUpdate(MessageSocket msgSocket) throws IOException {
        MessageSerializer.Message message;
        while ((message = msgSocket.readMessage()) != null) {
            if (Objects.requireNonNull(message.type()) == MessageSerializer.Type.UPDATE) {
                handleUpdate(message);
            };
        }
    }

    private static void handleUpdate(MessageSerializer.Message message) {
        String updateType = message.detail();
        if (updateType.equals("NEW-PRIMARY")) {
            if (suppressNextRefresh) {
                suppressNextRefresh = false;
                return;
            }
            refreshRequested = true;
        }
    }

    private static void shutdown() {
        try {
            if (observerSocket != null && !observerSocket.isClosed()) {
                observerSocket.close();
            }
        } catch (IOException ignored) {}

        pool.shutdownNow();
    }
}
