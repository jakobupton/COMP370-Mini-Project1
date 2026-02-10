package comp370.srms;

import java.lang.reflect.Method;

public final class ServerProcessTest {
    public static void main(String[] args) throws Exception {
        shouldParseDefaultConfig();
        shouldParseExplicitConfig();
        shouldRejectTooManyArgs();
        shouldRejectInvalidPort();
        shouldRejectInvalidHeartbeat();

        System.out.println("PASS: ServerProcessTest");
    }

    private static void shouldParseDefaultConfig() throws Exception {
        Object process = TestUtilities.newPrivateInstance(ServerProcess.class);
        Object config = invokeParseArgs(process, new String[]{});

        TestUtilities.assertEquals(
                "localhost",
                invokeConfigAccessor(config, "monitorHost"),
                "Default monitor host should be localhost");
        TestUtilities.assertEquals(
                3000,
                invokeConfigAccessor(config, "monitorPort"),
                "Default monitor port should be 3000");
        TestUtilities.assertEquals(
                1000,
                invokeConfigAccessor(config, "heartbeatIntervalMs"),
                "Default heartbeat should be 1000ms");
    }

    private static void shouldParseExplicitConfig() throws Exception {
        Object process = TestUtilities.newPrivateInstance(ServerProcess.class);
        Object config = invokeParseArgs(process, new String[]{"192.168.0.1", "4567", "2500"});

        TestUtilities.assertEquals(
                "192.168.0.1",
                invokeConfigAccessor(config, "monitorHost"),
                "Monitor host arg should be parsed");
        TestUtilities.assertEquals(
                4567,
                invokeConfigAccessor(config, "monitorPort"),
                "Monitor host port should be parsed");
        TestUtilities.assertEquals(
                2500,
                invokeConfigAccessor(config, "heartbeatIntervalMs"),
                "Monitor heartbeat should be parsed");
    }

    private static void shouldRejectTooManyArgs() throws Exception {
        Object process = TestUtilities.newPrivateInstance(ServerProcess.class);
        TestUtilities.assertThrows(
                IllegalArgumentException.class,
                () -> invokeParseArgs(process, new String[]{"a", "b", "c", "d"}),
                "parseArgs should reject too many arguments");
    }

    private static void shouldRejectInvalidPort() throws Exception {
        Object process = TestUtilities.newPrivateInstance(ServerProcess.class);
        TestUtilities.assertThrows(
                IllegalArgumentException.class,
                () -> invokeParseArgs(process, new String[]{"localhost", "70000", "1000"}),
                "parseArgs should reject invalid port values");
    }

    private static void shouldRejectInvalidHeartbeat() throws Exception {
        Object process = TestUtilities.newPrivateInstance(ServerProcess.class);
        TestUtilities.assertThrows(
                IllegalArgumentException.class,
                () -> invokeParseArgs(process, new String[]{"localhost", "3000", "50"}),
                "parseArgs should reject too-small heartbeat values");
    }

    private static Object invokeParseArgs(Object process, String[] args) throws Exception {
        Method parseArgs = TestUtilities.privateMethod(ServerProcess.class, "parseArgs", String[].class);
        return parseArgs.invoke(process, (Object) args);
    }

    private static Object invokeConfigAccessor(Object config, String accessor) throws Exception {
        Method m = TestUtilities.privateMethod(config.getClass(), accessor);
        return m.invoke(config);
    }

}
