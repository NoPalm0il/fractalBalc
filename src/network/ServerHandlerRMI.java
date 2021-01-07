package network;

import gui.GuiUpdate;
import network.shared.BalancerRMI;
import network.shared.ServerRMI;
import utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerHandlerRMI implements BalancerRMI {

    private final CopyOnWriteArrayList<ServerRMI> servers;
    private final GuiUpdate balcGUI;
    private final BufferedImage fractalImage;

    public ServerHandlerRMI(GuiUpdate balcGUI, BufferedImage fractalImage, CopyOnWriteArrayList<ServerRMI> servers) {
        this.balcGUI = balcGUI;
        this.fractalImage = fractalImage;
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

    @Override
    public void setRectFractalImg(int x, int y, int[][] colorBuffer) throws RemoteException {
        ImageUtils.paintBufferedImage(x, y, colorBuffer, fractalImage);
    }
}
