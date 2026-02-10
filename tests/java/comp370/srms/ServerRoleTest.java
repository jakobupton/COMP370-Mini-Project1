package comp370.srms;

public final class ServerRoleTest {
    public static void main(String[] args) {
        shouldDeserializeCaseInsensitiveRole();
        shouldSerializeRole();
        shouldRejectUnknownRole();
        shouldRejectEmptyRole();

        System.out.println("PASS: ServerRoleTest");
    }

    private static void shouldDeserializeCaseInsensitiveRole() {
        ServerRole role = ServerRole.deserialize("primary");
        TestUtilities.assertEquals(
                ServerRole.PRIMARY,
                role,
                "deserialize should be case-insensitive");
    }

    private static void shouldSerializeRole() {
        String serialized = ServerRole.BACKUP.serialize();
        TestUtilities.assertEquals(
                "BACKUP",
                serialized,
                "serialize should return enum name");
    }

    private static void shouldRejectUnknownRole() {
        TestUtilities.assertThrows(
                IllegalArgumentException.class,
                () -> ServerRole.deserialize("leader"),
                "Unknown roles must throw IllegalArgumentException");
    }

    private static void shouldRejectEmptyRole() {
        TestUtilities.assertThrows(
                IllegalArgumentException.class,
                () -> ServerRole.deserialize("   "),
                "Empty roles must throw IllegalArgumentException");
    }
}
