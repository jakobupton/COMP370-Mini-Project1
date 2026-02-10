package comp370.srms;

public final class SrmsNodeTest {
    public static void main(String[] args) {
        shouldParsePortInRange();
        shouldRejectInvalidPort();
        shouldParseHeartbeatInRange();
        shouldRejectInvalidHeartbeat();

        System.out.println("PASS: SrmsNodeTest");
    }

    private static void shouldParsePortInRange() {
        FakeNode node = new FakeNode();
        int port = node.parsePortValue("3000");
        TestUtilities.assertEquals(
                3000,
                port,
                "parsePort should parse valid port values");
    }

    private static void shouldRejectInvalidPort() {
        FakeNode node = new FakeNode();
        TestUtilities.assertThrows(
                IllegalArgumentException.class,
                () -> node.parsePortValue("70000"),
                "parsePort should reject out-of-range values");
    }

    private static void shouldParseHeartbeatInRange() {
        FakeNode node = new FakeNode();
        int heartbeat = node.parseHeartbeatValue("1500");
        TestUtilities.assertEquals(
                1500,
                heartbeat,
                "parseHeartbeat should parse valid heartbeat values");
    }

    private static void shouldRejectInvalidHeartbeat() {
        FakeNode node = new FakeNode();
        TestUtilities.assertThrows(
                IllegalArgumentException.class,
                () -> node.parseHeartbeatValue("50"),
                "parseHeartbeat should reject too-small values");
    }

    private static final class FakeNode extends SrmsNode {
        private FakeNode() {
            super("TEST");
        }

        private int parsePortValue(String raw) {
            return parsePort(raw);
        }

        private int parseHeartbeatValue(String raw) {
            return parseHeartbeat(raw);
        }
    }
}
