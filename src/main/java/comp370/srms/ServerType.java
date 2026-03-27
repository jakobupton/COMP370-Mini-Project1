package comp370.srms;

public final class ServerType {
    private final ServerRole role;
    private final String remoteAddress;
    private final int portForClient;

    public ServerType(ServerRole role, String remoteAddress, int portForClient) {
        this.role = role;
        this.remoteAddress = remoteAddress;
        this.portForClient = portForClient;
    }

    public ServerRole role() {
        return role;
    }

    public ServerType withRole(ServerRole newRole) {
        return new ServerType(newRole, this.remoteAddress, this.portForClient);
    }
    
    public String remoteAddress() {
        return remoteAddress;
    }

    public int portForClient() {
        return portForClient;
    }

    @Override
    public String toString(){
        return "ServerType{role=" + role + ", remoteAddress='" + remoteAddress + "', portForClient=" + portForClient + "}";
    }

}
