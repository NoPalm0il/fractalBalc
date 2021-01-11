package network;

import gui.GuiUpdate;
import network.shared.ServerRMI;
import utils.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {

    private final DataInputStream clientInputStream;
    private final DataOutputStream clientOutputStream;
    private final ArrayList<ServerRMI> servers;
    private final GuiUpdate balcGUI;

    public ClientHandler(DataInputStream clientInputStream, DataOutputStream clientOutputStream, ArrayList<ServerRMI> servers, GuiUpdate balcGUI) {
        this.clientInputStream = clientInputStream;
        this.clientOutputStream = clientOutputStream;
        this.servers = servers;
        this.balcGUI = balcGUI;
    }

    @Override
    public void run() {
        try {
            String rawString = clientInputStream.readUTF();

            String[] fractalParams = rawString.split(" ");
            int dimX = Integer.parseInt(fractalParams[4]);
            int dimY = Integer.parseInt(fractalParams[5]);
            int totalFrames = Integer.parseInt(fractalParams[6]);

            int[][][][] fractalFrameColors = new int[servers.size()][totalFrames][dimY][dimX];

            int totalServers = servers.size();
            int serverFrames = totalFrames / totalServers;
            int[][] framesPerServer;

            if(totalServers % totalFrames != 0)
                framesPerServer = new int[totalServers][serverFrames + 1];
            else
                framesPerServer = new int[totalServers][serverFrames];

            if(totalServers != 1) {
                int restFrames = 0;
                for (int k = 0; k < totalServers; k++) {
                    for (int j = 0; j < totalFrames; j++) {
                        if (j % totalServers == k)
                            framesPerServer[k][restFrames++] = j;
                    }
                    restFrames = 0;
                }
            } else
                for (int i = 0; i < serverFrames; i++) framesPerServer[0][i] = i;
            ExecutorService exe = Executors.newFixedThreadPool(servers.size());

            int i = 0;
            for (ServerRMI server : servers) {
                server.setFractalParams(rawString);
                server.setIndexes(framesPerServer[i++]);
                server.setTotalFrames(totalFrames);
            }
            AtomicInteger indexer = new AtomicInteger();
            exe.execute(() -> {
                for (ServerRMI server : servers) {
                    try {
                        fractalFrameColors[indexer.getAndIncrement()] = server.generateFractal();
                    } catch (RemoteException re) {
                        balcGUI.onException("", re);
                    }
                }
            });
            exe.shutdown();
            exe.awaitTermination(5, TimeUnit.HOURS);
            // fractalImg = ImageUtils.colorArrayToImage(fractalColors);
            sendFractal(fractalFrameColors);

            balcGUI.onDisplay(Color.GREEN, "fractal sent to client");
            clientInputStream.close();
        } catch (IOException | InterruptedException io) {
            balcGUI.onException("", io);
        }
    }

    private void sendFractal(int[][][][] fractalFrameColors) throws IOException, InterruptedException {
        Frame jf = new JFrame("shit");
        JLabel lb = new JLabel();
        jf.add(lb);
        jf.setVisible(true);


        // foreach server
        for (int[][][] fractalFrameColor : fractalFrameColors) {
            BufferedImage fractalImg = null;
            // foreach frame
            for (int j = 0; j < fractalFrameColors[0].length - 1; j++) {
                fractalImg = ImageUtils.colorArrayToImage(fractalFrameColor[j]);
                lb.setIcon(new ImageIcon(fractalImg));
                jf.pack();
                // todo: acaba isto palhaço
                Thread.sleep(100);
            }
            clientOutputStream.write(ImageUtils.imageToByteArray(fractalImg));
            clientOutputStream.flush();
        }
    }
}
