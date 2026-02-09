package comp370.srms;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public final class MessageSocketTest {
    public static void main(String[] args) throws Exception {
        shouldSendAndReceiveMessage();
        shouldReturnNullWhenPeerClosesConnection();

        System.out.println("PASS: MessageSocketTest");
    }


    private static void shouldSendAndReceiveMessage() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            AtomicReference<Throwable> serverError = new AtomicReference<>(null);

            Thread serverThread = getThread(serverSocket, serverError);

            try (MessageSocket client = MessageSocket.connect("localhost", port)) {
                client.send(MessageSerializer.serializeHello());
                MessageSerializer.Message response = client.readMessage();
                TestUtilities.assertTrue(response != null, "Client should receive a response");
                TestUtilities.assertEquals(
                        MessageSerializer.Type.ACK,
                        response.type(),
                        "Client should receive ACK");
            }

            serverThread.join(1000);
            if (serverThread.isAlive()) {
                TestUtilities.fail("Server thread did not finish");
            }
            if (serverError.get() != null) {
                throw new RuntimeException(serverError.get());
            }
        }
    }

    private static Thread getThread(ServerSocket serverSocket, AtomicReference<Throwable> serverError) {
        Thread serverThread = new Thread(() -> {
            try (Socket accepted = serverSocket.accept();
                 MessageSocket socket = MessageSocket.fromSocket(accepted)) {
                MessageSerializer.Message incoming = socket.readMessage();
                if (incoming == null) {
                    throw new IOException("Expected HELLO message, got null");
                }
                TestUtilities.assertEquals(
                        MessageSerializer.Type.HELLO,
                        incoming.type(),
                        "Server should receive HELLO");
                socket.send(MessageSerializer.serializeAck());
            } catch (Throwable t) {
                serverError.set(t);
            }
        });

        serverThread.start();
        return serverThread;
    }

    private static void shouldReturnNullWhenPeerClosesConnection() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try (Socket accepted = serverSocket.accept()) {
                    // Close immediately without sending data.
                } catch (IOException ignored) {
                    // ignored
                }
            });

            serverThread.start();

            try (MessageSocket client = MessageSocket.connect("localhost", port)) {
                MessageSerializer.Message message = client.readMessage();
                TestUtilities.assertEquals(
                        null,
                        message,
                        "readMessage should return null when peer closes");
            }

            serverThread.join(1000);
            TestUtilities.assertTrue(!serverThread.isAlive(), "Server thread should finish");
        }
    }
}
