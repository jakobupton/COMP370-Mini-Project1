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
            System.out.println(msg.type());
            System.out.println(msg.detail());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }
}