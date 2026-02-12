package comp370.srms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class AdminConsole {

    private static final Path MONITOR_LOG = Path.of("logs/MONITOR.log");

    public static void main(String[] args) {
        if (!Files.exists(MONITOR_LOG)) {
            System.out.println("No monitor log found. Start the monitor first.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(MONITOR_LOG);

            System.out.println("=== RECENT LOGS ===");
            lines.stream()
                    .skip(Math.max(0, lines.size() - 10))
                    .forEach(System.out::println);

            System.out.println("\n=== SERVER HEALTH ===");

            Map<String, Instant> lastHeartbeat = new HashMap<>();
            Map<String, String> roles = new HashMap<>();

            for (String line : lines) {
                if (line.contains("Assigned role")) {
                    // Example: Assigned role PRIMARY to s1
                    String[] parts = line.split(" ");
                    String role = parts[parts.length - 3];
                    String server = parts[parts.length - 1];
                    roles.put(server, role);
                }

                if (line.contains("HEARTBEAT")) {
                    String[] parts = line.split(" ");
                    String server = parts[parts.length - 2];
                    lastHeartbeat.put(server, Instant.now());
                }
            }

            if (roles.isEmpty()) {
                System.out.println("No servers registered yet.");
            } else {
                for (String server : roles.keySet()) {
                    Instant hb = lastHeartbeat.get(server);
                    String status = (hb == null) ? "UNKNOWN" : "ALIVE";
                    System.out.printf("%s → %s → %s%n", server, roles.get(server), status);
                }
            }

            System.out.println("\n(Admin actions like manual failover pending Issue #4)");

        } catch (IOException e) {
            System.out.println("Failed to read monitor log.");
        }
    }
}
