package comp370.srms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class MessageSocket implements AutoCloseable {
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;

    private MessageSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public static MessageSocket connect(String host, int port) throws IOException {
        return new MessageSocket(new Socket(host, port));
    }

    public static MessageSocket fromSocket(Socket socket) throws IOException {
        return new MessageSocket(socket);
    }

    public String remoteAddress() {
        return String.valueOf(socket.getRemoteSocketAddress());
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public synchronized void send(String serializedMessage) throws IOException {
        writer.write(serializedMessage);
        writer.newLine();
        writer.flush();
    }

    public MessageSerializer.Message readMessage() throws IOException {
        String raw = reader.readLine();
        if (raw == null) {
            return null;
        }
        return MessageSerializer.deserialize(raw);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
