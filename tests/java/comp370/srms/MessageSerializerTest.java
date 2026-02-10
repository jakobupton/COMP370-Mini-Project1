package comp370.srms;

public final class MessageSerializerTest {
    public static void main(String[] args) {
        shouldSerializeAssign();
        shouldDeserializeAssignAsEqualRecord();
        shouldDeserializeHeartbeat();
        shouldMarkInvalidRoleAsInvalidMessage();
        shouldParseErrorDetail();

        System.out.println("PASS: MessageSerializerTest");
    }

    private static void shouldSerializeAssign() {
        String serialized = MessageSerializer.serializeAssign("s9", ServerRole.BACKUP);
        TestUtilities.assertEquals(
                "ASSIGN s9 BACKUP",
                serialized,
                "serializeAssign should format properly");
    }

    private static void shouldDeserializeAssignAsEqualRecord() {
        MessageSerializer.Message expected = MessageSerializer.Message.assign("s1", ServerRole.PRIMARY);
        MessageSerializer.Message actual = MessageSerializer.deserialize("ASSIGN s1 PRIMARY");

        TestUtilities.assertEquals(
                expected,
                actual,
                "record equality should work for deserialized ASSIGN messages");
    }

    private static void shouldDeserializeHeartbeat() {
        MessageSerializer.Message message = MessageSerializer.deserialize("HEARTBEAT s2 12345");
        TestUtilities.assertEquals(
                MessageSerializer.Type.HEARTBEAT,
                message.type(),
                "HEARTBEAT should parse as HEARTBEAT type");
        TestUtilities.assertEquals(
                "s2",
                message.serverId(),
                "HEARTBEAT should include server id");
        TestUtilities.assertEquals(
                12345L,
                message.timestampMs(),
                "HEARTBEAT should parse timestamp");
    }

    private static void shouldMarkInvalidRoleAsInvalidMessage() {
        MessageSerializer.Message message = MessageSerializer.deserialize("ASSIGN s3 FAKE-ROLE");
        TestUtilities.assertEquals(
                MessageSerializer.Type.INVALID,
                message.type(),
                "Unknown role should produce INVALID message");
    }

    private static void shouldParseErrorDetail() {
        MessageSerializer.Message message = MessageSerializer.deserialize("ERROR monitor down");
        TestUtilities.assertEquals(
                MessageSerializer.Type.ERROR,
                message.type(),
                "ERROR should parse as ERROR type");
        TestUtilities.assertEquals(
                "monitor down",
                message.detail(),
                "ERROR detail should be parsed");
    }
}
