package comp370.srms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class Client {
    public static String currentPrimary = "";
    public static void main(String[] args) {
        String MonitorIP = "127.0.0.1";
        int MonitorPort = 3001;
        String primaryRemoteAddr;

        try (Socket MonitorConn = new Socket(MonitorIP, MonitorPort)){
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(MonitorConn.getInputStream())
            );
            PrintWriter out = new PrintWriter(
                    MonitorConn.getOutputStream(), true
            );
            String req = MessageSerializer.serializeGetPrimary();
            out.println(req);
            String response = in.readLine();
            MessageSerializer.Message msg = MessageSerializer.deserialize(response);
            primaryRemoteAddr = msg.detail();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        String primaryIp = primaryRemoteAddr.split(":")[0];
        primaryIp = primaryIp.replace("/", "");
        int primaryPort = Integer.parseInt(primaryRemoteAddr.split(":")[1]);
        try (MessageSocket msg = MessageSocket.connect(primaryIp, primaryPort)) {
            msg.send(MessageSerializer.serializeProcess());
            MessageSerializer.Message processingMessage = msg.readMessage();
            if (processingMessage == null) {
                return;
            }
            if (processingMessage.type() != MessageSerializer.Type.PROCESSING) {
                msg.send(MessageSerializer.serializeError("Expected PROCESSING, got " + processingMessage.type()));
            }
            System.out.println("Server is processing!");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }
    }
}