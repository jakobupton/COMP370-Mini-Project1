package comp370.srms;

import java.util.Locale;

public final class MessageSerializer {
    private MessageSerializer() {
    }

    public static String serializeHello() {
        return "HELLO";
    }

    public static String serializePort(int port) {
        return "PORT " + port;
    }

    public static String serializeAssign(String serverId, ServerRole role) {
        return "ASSIGN " + serverId + " " + role.serialize();
    }

    public static String serializeHeartbeat(String serverId, long timestampMs) {
        return "HEARTBEAT " + serverId + " " + timestampMs;
    }

    public static String serializeProcess() {
        return "PROCESS";
    }

    public static String serializeProcessing() {
        return "PROCESSING";
    }

    public static String serializePrimary(String remoteAddress, String serverid) {
        return "PRIMARY " + remoteAddress + " " + serverid;
    }

    public static String serializeStop() {
        return "STOP";
    }

    public static String serializePing() {
        return "PING";
    }

    public static String serializeGetPrimary() {
        return "GETPRIMARY";
    }

    public static String serializeAck() {
        return "ACK";
    }

    public static String serializeError(String detail) {
        return "ERROR " + detail;
    }

    public static String serializePromote(String serverId) {
        return "PROMOTE " + serverId;
    }

    public static Message deserialize(String raw) {
        if (raw == null) {
            return Message.invalid("Message was null");
        }

        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Message.invalid("Message was empty");
        }

        String[] parts = trimmed.split("\\s+");
        String command = parts[0].toUpperCase(Locale.ROOT);

        return switch (command) {
            case "HELLO" -> parts.length == 1
                    ? Message.hello()
                    : Message.invalid("HELLO takes no arguments");
            case "ASSIGN" -> parseAssign(parts);
            case "HEARTBEAT" -> parseHeartbeat(parts);
            case "ACK" -> parts.length == 1
                    ? Message.ack()
                    : Message.invalid("ACK takes no arguments");
            case "ERROR" -> Message.error(extractErrorDetail(trimmed));
            case "GETPRIMARY" -> parts.length == 1
                    ? Message.getprimary()
                    : Message.invalid("GETPRIMARY takes no arguments");
            case "PROCESS" -> parts.length == 1
                    ? Message.process()
                    : Message.invalid("PROCESS takes no arguments");
            case "PROCESSING" -> parts.length == 1
                    ? Message.processing()
                    : Message.invalid("PROCESSING takes no arguments");
            case "PING" -> parts.length == 1
                    ? Message.ping()
                    : Message.invalid("PING takes no arguments");
            case "PRIMARY" -> parsePrimary(parts);
            case "PROMOTE" -> parsePromote(parts);
            case "PORT" -> parsePort(parts);
            case "STOP" -> parts.length == 1
                    ? Message.stop()
                    : Message.invalid("STOP takes no arguments");
            default -> Message.invalid("Unknown command: " + command);
        };
    }

    private static Message parsePrimary(String[] parts) {
        if (parts.length != 3) {
            return Message.invalid("PRIMARY format: PRIMARY <server-address> <server-id>");
        }
        return Message.primary(parts[1], parts[2]);
    }

    private static Message parsePromote(String[] parts) {
        if (parts.length != 2) {
            return Message.invalid("PROMOTE format: PROMOTE <server-id>");
        }
        return Message.promote(parts[1]);
    }

    private static Message parsePort(String[] parts) {
        if (parts.length != 2) {
            return Message.invalid("PORT format: PORT <port-number>");
        }
        return Message.port(parts[1]);
    }

    private static Message parseHeartbeat(String[] parts) {
        if (parts.length != 3) {
            return Message.invalid("HEARTBEAT format: HEARTBEAT <server-id> <timestamp-ms>");
        }
        try {
            long timestamp = Long.parseLong(parts[2]);
            return Message.heartbeat(parts[1], timestamp);
        } catch (NumberFormatException ex) {
            return Message.invalid("HEARTBEAT timestamp must be a number");
        }
    }

    private static Message parseAssign(String[] parts) {
        if (parts.length != 3) {
            return Message.invalid("ASSIGN format: ASSIGN <server-id> <role>");
        }
        try {
            ServerRole role = ServerRole.deserialize(parts[2]);
            return Message.assign(parts[1], role);
        } catch (IllegalArgumentException ex) {
            return Message.invalid("Invalid role in ASSIGN: " + parts[2]);
        }
    }

    private static String extractErrorDetail(String trimmed) {
        if (trimmed.length() <= "ERROR".length()) {
            return "Unknown error";
        }
        return trimmed.substring("ERROR".length()).trim();
    }

    public enum Type {
        HELLO,
        ASSIGN,
        HEARTBEAT,
        ACK,
        ERROR,
        INVALID,
        GETPRIMARY,
        PRIMARY,
        PROCESSING,
        PROCESS,
        PROMOTE,
        PING,
        PORT,
        STOP
    }

    public record Message(
            Type type,
            String serverId,
            ServerRole role,
            long timestampMs,
            String detail) {

        public static Message hello() {
            return new Message(Type.HELLO, "", null, -1L, "");
        }

        public static Message assign(String serverId, ServerRole role) {
            return new Message(Type.ASSIGN, serverId, role, -1L, "");
        }

        public static Message heartbeat(String serverId, long timestampMs) {
            return new Message(Type.HEARTBEAT, serverId, null, timestampMs, "");
        }

        public static Message stop() {
            return new Message(Type.STOP, "", null, -1L, "");
        }

        public static Message ping() {
            return new Message(Type.PING, "", null, -1L, "");
        }

        public static Message promote(String serverId){
            return new Message(Type.PROMOTE, serverId, null, -1L, "");
        }

        public static Message getprimary() {
            return new Message(Type.GETPRIMARY, "", null, -1L, "");
        }

        public static Message port(String port) {
            return new Message(Type.PORT, "", null, -1L, port);
        }

        public static Message process() {
            return new Message(Type.PROCESS, "", null, -1L, "");
        }

        public static Message processing() {
            return new Message(Type.PROCESSING, "", null, -1L, "");
        }

        public static Message primary(String remoteAddr, String serverId) {
            return new Message(Type.PRIMARY, serverId, null, -1L, remoteAddr);
        }

        public static Message ack() {
            return new Message(Type.ACK, "", null, -1L, "");
        }

        public static Message error(String detail) {
            return new Message(Type.ERROR, "", null, -1L, detail);
        }

        public static Message invalid(String detail) {
            return new Message(Type.INVALID, "", null, -1L, detail);
        }
    }
}
