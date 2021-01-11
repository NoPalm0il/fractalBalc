package network;

import gui.GuiUpdate;
import network.shared.BalancerRMI;
import network.shared.ServerRMI;

import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;

public class ServerHandlerRMI implements BalancerRMI {

    private final ArrayList<ServerRMI> servers;
    private final GuiUpdate balcGUI;

    public ServerHandlerRMI(GuiUpdate balcGUI, ArrayList<ServerRMI> servers) {
        this.balcGUI = balcGUI;
        this.servers = servers;
    }

    @Override
    public void serverConnected(ServerRMI serverRMI) throws RemoteException {
        try {
            servers.add(serverRMI);
            balcGUI.onDisplay(Color.GREEN, "Server connected " + RemoteServer.getClientHost());
        } catch (ServerNotActiveException sne) {
            balcGUI.onException("", sne);
        }
    }
}
