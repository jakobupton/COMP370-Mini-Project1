package comp370.srms;

import java.lang.reflect.Method;

public final class ServerMonitorTest {
    public static void main(String[] args) throws Exception {
        shouldAssignIncrementingServerIds();
        shouldAssignPrimaryThenBackupRoles();
        shouldRejectHeartbeatServerIdMismatch();
        shouldAcknowledgeErrorMessages();
        shouldRejectUnsupportedMessageTypes();

        System.out.println("PASS: ServerMonitorTest");
    }

    private static void shouldAssignIncrementingServerIds() throws Exception {
        ServerMonitor.resetInstanceForTests();
        Object monitor = ServerMonitor.getInstance();
        Method assignServerId = TestUtilities.privateMethod(ServerMonitor.class, "assignServerId");

        String first = (String) assignServerId.invoke(monitor);
        String second = (String) assignServerId.invoke(monitor);

        TestUtilities.assertEquals("s1", first, "First assigned id should be s1");
        TestUtilities.assertEquals("s2", second, "Second assigned id should be s2");
    }

    private static void shouldAssignPrimaryThenBackupRoles() throws Exception {
        ServerMonitor.resetInstanceForTests();
        Object monitor = ServerMonitor.getInstance();
        Method assignRole = TestUtilities.privateMethod(ServerMonitor.class, "assignRole", String.class);

        ServerRole first = (ServerRole) assignRole.invoke(monitor, "s1");
        ServerRole second = (ServerRole) assignRole.invoke(monitor, "s2");

        TestUtilities.assertEquals(ServerRole.PRIMARY, first, "First role should be PRIMARY");
        TestUtilities.assertEquals(ServerRole.BACKUP, second, "Second role should be BACKUP");
    }

    private static void shouldRejectHeartbeatServerIdMismatch() throws Exception {
        ServerMonitor.resetInstanceForTests();
        Object monitor = ServerMonitor.getInstance();
        Method process = TestUtilities.privateMethod(
                ServerMonitor.class,
                "processServerMessage",
                String.class,
                MessageSerializer.Message.class);

        String response = (String) process.invoke(
                monitor,
                "s1",
                MessageSerializer.Message.heartbeat("s2", 42L));

        TestUtilities.assertEquals(
                MessageSerializer.serializeError("Heartbeat server-id mismatch"),
                response,
                "Heartbeat with mismatched id should return error");
    }

    private static void shouldAcknowledgeErrorMessages() throws Exception {
        ServerMonitor.resetInstanceForTests();
        Object monitor = ServerMonitor.getInstance();
        Method process = TestUtilities.privateMethod(
                ServerMonitor.class,
                "processServerMessage",
                String.class,
                MessageSerializer.Message.class);

        String response = (String) process.invoke(
                monitor,
                "s1",
                MessageSerializer.Message.error("simulated error"));

        TestUtilities.assertEquals(
                MessageSerializer.serializeAck(),
                response,
                "ERROR message should be acknowledged");
    }

    private static void shouldRejectUnsupportedMessageTypes() throws Exception {
        ServerMonitor.resetInstanceForTests();
        Object monitor = ServerMonitor.getInstance();
        Method process = TestUtilities.privateMethod(
                ServerMonitor.class,
                "processServerMessage",
                String.class,
                MessageSerializer.Message.class);

        String response = (String) process.invoke(
                monitor,
                "s1",
                MessageSerializer.Message.hello());

        TestUtilities.assertEquals(
                MessageSerializer.serializeError("Unsupported message type: HELLO"),
                response,
                "Unsupported message types should return error");
    }

}
