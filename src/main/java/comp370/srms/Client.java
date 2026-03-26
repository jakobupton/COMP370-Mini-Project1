package comp370.srms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Client {
    private static MessageSocket Primary;
    private static MessageSocket Monitor;
    private static boolean primaryConnected = false;
    private static volatile boolean primaryFail = false;
    private static boolean monitorConnected = false;
    private static volatile boolean monitorFail = false;

    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        monitorConnected = ConnectToMonitor();
        if (!monitorConnected) {
            System.out.println("Client unable to connect to monitor.");
            return;
        }

        primaryConnected = ConnectToPrimary();
        if (!primaryConnected) {
            System.out.println("Client unable to connect to primary.");
            return;
        }

        pool.submit(() -> {
            while(true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                monitorConnected = CheckMonitor();
                primaryConnected = CheckPrimary();

                if (!monitorConnected) {
                    System.out.println("Monitor disconnected, attempting to reconnect...");
                    monitorConnected = ConnectToMonitor();
                    if (!monitorConnected) {
                        monitorFail = true;
                        System.out.println("Client unable to connect to monitor.");
                    } else {
                        monitorFail = false;
                    }
                }
                if (!primaryConnected) {
                    System.out.println("Primary disconnected, attempting to reconnect...");
                    primaryConnected = ConnectToPrimary();
                    if (!primaryConnected) {
                        primaryFail = true;
                        System.out.println("Client unable to connect to primary.");
                        return;
                    } else {
                        primaryFail = false;
                    }
                }
            }
        });

        pool.submit(() -> {
           while(!primaryFail) {
               try {
                   Thread.sleep(1000);
               } catch (InterruptedException e) {
                   System.out.println(e);
                   return;
               }
               if (primaryConnected) {
                   System.out.println("Sending processing request...");
                   try {
                       Primary.send(MessageSerializer.serializeProcess());
                       MessageSerializer.Message reply = Primary.readMessage();
                       assert reply != null;
                       System.out.println(reply.type());
                   } catch (Exception e) {
                       System.out.println(e);
                   }

               } else {
                   System.out.println("Waiting for primary...");
               }
           }
           System.out.println("Primary Failed: " + primaryFail);
           System.out.println("Monitor Failed: " + monitorFail);
        });
    }

    private static boolean ConnectToMonitor() {
        int attempts = 0;
        int maxAttempts = 5;
        String MonitorIP = "127.0.0.1";
        int MonitorPort = 3001;
        while (attempts < maxAttempts) {
            try {
                Monitor = MessageSocket.connect(MonitorIP, MonitorPort);
                return true;
            } catch (Exception ex) {
//                System.out.println(ex);
                attempts++;
            }
        }
        return false;
    }

    private static boolean ConnectToPrimary() {
        int attempts = 0;
        int maxAttempts = 5;
        while (attempts < maxAttempts) {
            try {
                Monitor.send(MessageSerializer.serializeGetPrimary());
                MessageSerializer.Message reply = Monitor.readMessage();
                assert reply != null;
                String primaryAddr = reply.detail();
                primaryAddr = primaryAddr.replaceAll("/", "");
                String[] primaryAddrParts = primaryAddr.split(":");
                int primaryPort = Integer.parseInt(primaryAddrParts[1]);
                Primary = MessageSocket.connect(primaryAddrParts[0], primaryPort);
                return true;
            } catch (Exception e) {
//                System.out.println(e);
                attempts++;
            }
        }
        return false;
    }

    private static boolean CheckMonitor() {
        try {
            Monitor.send(MessageSerializer.serializePing());
            Monitor.readMessage();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean CheckPrimary() {
        try {
            Primary.send(MessageSerializer.serializePing());
            Primary.readMessage();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}