package network;

import gui.GuiUpdate;
import network.shared.ServerRMI;
import utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    private final DataInputStream clientInputStream;
    private final DataOutputStream clientOutputStream;
    private final CopyOnWriteArrayList<ServerRMI> servers;
    private final GuiUpdate balcGUI;
    private BufferedImage fractalImg;

    public ClientHandler(DataInputStream clientInputStream, DataOutputStream clientOutputStream, CopyOnWriteArrayList<ServerRMI> servers, GuiUpdate balcGUI, BufferedImage fractalImage) {
        this.clientInputStream = clientInputStream;
        this.clientOutputStream = clientOutputStream;
        this.servers = servers;
        this.balcGUI = balcGUI;
        this.fractalImg = fractalImage;
    }

    @Override
    public void run() {
        try {
            String rawString = clientInputStream.readUTF();

            String[] fractalParams = rawString.split(" ");
            int dimX = Integer.parseInt(fractalParams[4]);
            int dimY = Integer.parseInt(fractalParams[5]);
            int work = dimX / servers.size();

            fractalImg = new BufferedImage(dimX, dimY, BufferedImage.TYPE_INT_RGB);
            int[][] fractalColors = null;

            for (ServerRMI server : servers) {
                try {
                    server.setFractalParams(rawString);
                    // with the generate it will write to the buffered image
                    fractalColors = server.generateFractal();
                } catch (RemoteException remoteException) {
                    balcGUI.onException("Server not connected, removed from table", remoteException);
                    servers.remove(server);
                }
            }
            assert fractalColors != null;
            fractalImg = ImageUtils.colorArrayToImage(fractalColors);
            clientOutputStream.write(ImageUtils.imageToByteArray(fractalImg));

            balcGUI.onDisplay(Color.GREEN, "fractal sent to client");
            clientInputStream.close();
        } catch (IOException io) {
            balcGUI.onException("", io);
        } finally {
            balcGUI.onDisplay(Color.YELLOW, "client stopped");
        }
    }

}
