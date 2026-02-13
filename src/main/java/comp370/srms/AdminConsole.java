package comp370.srms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class AdminConsole {

    private static final Path MONITOR_LOG = Path.of("logs/MONITOR.log");
    private static final Scanner scanner = new Scanner(System.in);
    private static String currentPrimaryID = "";
    private static String currentPrimaryAddr = "";

    public static void main(String[] args) {
        if (!Files.exists(MONITOR_LOG)) {
            System.out.println("No monitor log found. Start the monitor first.");
            return;
        }

        boolean exit = false;
        while (!exit) {
            List<String> values = Arrays.asList("1","2","3","4");
            String selection = Prompt("1. View most recent logs\n2. Check server status\n3. Perform manual failover (Stops current primary)\n4. Exit", values);
            switch (selection) {
                case "1":
                    ShowRecentLogs();
                    break;
                case "2":
                    ShowServerStatus();
                    break;
                case "3":
                    Failover();
                    break;
                case "4":
                    exit = true;
            }
        }
        scanner.close();
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
        Line();
    }

    private static boolean CheckMonitorConnection() throws IOException {
        try (MessageSocket Monitor = MessageSocket.connect("127.0.0.1", 3001)) {
            Monitor.send(MessageSerializer.serializePing());
            MessageSerializer.Message PingMessage = Monitor.readMessage();
            if (PingMessage == null) {
                return false;
            }
            if (PingMessage.type() != MessageSerializer.Type.PING) {
                return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean CheckPrimaryConnection() throws IOException {
        try (MessageSocket Monitor = MessageSocket.connect("127.0.0.1", 3001)) {
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
    }

    private static boolean ShutdownPrimary() throws IOException {
        try (MessageSocket Monitor = MessageSocket.connect("127.0.0.1", 3001)) {
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
        return acceptableValues.contains(input);
    }
}
