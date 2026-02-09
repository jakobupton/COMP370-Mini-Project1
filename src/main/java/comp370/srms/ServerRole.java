package comp370.srms;

import java.util.Locale;

public enum ServerRole {
    PRIMARY,
    BACKUP;

    public static ServerRole deserialize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Role is missing");
        }
        return ServerRole.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    public String serialize() {
        return name();
    }
}
