package comp370.srms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class Client {
    public static String currentPrimary = "";
    private static MessageSocket Primary;
    private static String PrimaryIP;
    private static int PrimaryPort;
    private static int MaxRetryAttempts = 5;
    private static boolean connected = false;
    public static void main(String[] args) {
        int attempt = 0;
        while (attempt < MaxRetryAttempts) {
            while (connected) {
                try (MessageSocket Primary = MessageSocket.connect(PrimaryIP, PrimaryPort)) {
                    attempt = 0;
                    while (!Primary.isClosed()) {
                        Primary.send(MessageSerializer.serializeProcess());
                        MessageSerializer.Message ProcessingMessage = Primary.readMessage();
                        if (ProcessingMessage == null) {
                            continue;
                        }
                        if (ProcessingMessage.type() != MessageSerializer.Type.PROCESSING) {
                            Primary.send(MessageSerializer.serializeError("Expected PROCESSING, got " + ProcessingMessage.type()));
                        }
                        System.out.println("Server is processing!");
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    connected = false;
                    System.out.println("Error: " + e.getMessage());
                }
            }

            try {
                GetPrimaryAddress();
            } catch (Exception e) {
                attempt++;
                System.out.println("Attempt " + attempt + " failed: " + e.getMessage());
                if (attempt >= MaxRetryAttempts) {
                    System.out.println("Failed to connect to Primary after " + attempt + " attempts.");
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {}
            }

        }
    }

    private static void GetPrimaryAddress() throws IOException {
        String MonitorIP = "127.0.0.1";
        int MonitorPort = 3001;
        try (Socket MonitorConn = new Socket(MonitorIP, MonitorPort)) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(MonitorConn.getInputStream())
            );
            PrintWriter out = new PrintWriter(
                    MonitorConn.getOutputStream(), true
            );
            String request = MessageSerializer.serializeGetPrimary();
            out.println(request);
            String response = in.readLine();
            MessageSerializer.Message message = MessageSerializer.deserialize(response);
            String primaryRemoteAddr = message.detail();
            String[] parts = primaryRemoteAddr.split(":");
            PrimaryIP = parts[0].replace("/", "");
            PrimaryPort = Integer.parseInt(parts[1]);
            connected = true;
        }
    }
}