package comp370.srms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class LogWriter {
    private static final Object LOCK = new Object();
    private static final Path LOG_DIR = Path.of("logs");

    private LogWriter() {}

    static void append(String nodeLabel, String line) {
        try {
            synchronized (LOCK) {
                if (!Files.exists(LOG_DIR)) {
                    Files.createDirectories(LOG_DIR);
                }

                String fileName = nodeLabel;
                if ("SERVER".equals(nodeLabel)) {
                    long pid = ProcessHandle.current().pid();
                    fileName = nodeLabel + "-" + pid;
                }

                Path file = LOG_DIR.resolve(fileName + ".log");
                Files.writeString(
                        file,
                        line + System.lineSeparator(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }
        } catch (IOException ignored) {}
    }
}
