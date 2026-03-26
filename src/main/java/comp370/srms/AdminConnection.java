package comp370.srms;

public class AdminConnection implements Observer{
    MessageSocket conn;
    AdminConnection(String adminIP, int adminPort) {
        try {
            this.conn = MessageSocket.connect(adminIP, adminPort);
        } catch (Exception ex) {
            this.conn = null;
        }
    }

    public void update(String updateType) {
        try {
            conn.send(MessageSerializer.serializeUpdate(updateType));
        } catch (Exception ignored) {}
    }
}
