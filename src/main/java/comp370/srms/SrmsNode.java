package comp370.srms;

import java.time.Instant;

abstract class SrmsNode {
    private final String nodeLabel;
    private volatile boolean running = true;

    protected SrmsNode(String nodeLabel) {
        this.nodeLabel = nodeLabel;
    }

    protected final boolean isRunning() {
        return running;
    }

    protected final void requestStop() {
        running = false;
    }

    protected final void attachShutdownHook(Runnable onShutdown) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            onShutdown.run();
        }));
    }

    protected final int parseIntInRange(String raw, int min, int max, String label) {
        try {
            int value = Integer.parseInt(raw);
            if (value < min || value > max) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + label + ": " + raw, ex);
        }
    }

    protected final int parsePort(String raw) {
        return parseIntInRange(raw, 1, 65535, "Port");
    }

    protected final int parseHeartbeat(String raw){
        return parseIntInRange(raw, 100, 60_000, "Heartbeat ms");
    }

    protected final void sleepQuietly(long sleepMs) {
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected final void log(String message) {
        String line = String.format("%s [%s] %s", Instant.now(), nodeLabel, message);
        System.out.println(line);
        LogWriter.append(nodeLabel, line);
    }
}
