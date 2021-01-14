package network;

import gui.GuiUpdate;
import network.shared.ServerRMI;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {

    private final DataInputStream clientInputStream;
    private final ObjectOutputStream clientOutputStream;
    private final ArrayList<ServerRMI> servers;
    private final GuiUpdate balcGUI;

    public ClientHandler(DataInputStream clientInputStream, ObjectOutputStream clientOutputStream, ArrayList<ServerRMI> servers, GuiUpdate balcGUI) {
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

            byte[][][] fractalFrameColors = new byte[servers.size()][totalFrames][1024 * 1024];

            int totalServers = servers.size();
            int serverFrames = totalFrames / totalServers;
            int[][] framesPerServer;

            if (totalFrames % totalServers != 0)
                framesPerServer = new int[totalServers][serverFrames + 1];
            else
                framesPerServer = new int[totalServers][serverFrames];

            if (totalServers != 1) {
                int restFrames = 0;
                for (int k = 0; k < totalServers; k++) {
                    for (int j = 0; j < totalFrames; j++) {
                        if (j % totalServers == k)
                            framesPerServer[k][restFrames++] = j;
                    }
                    restFrames = 0;
                }
            } else
                for (int i = 0; i < serverFrames; i++)
                    framesPerServer[0][i] = i;
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

            balcGUI.onDisplay(Color.GREEN, "received frames from servers, sending to client...");

            indexer.set(0);
            exe = Executors.newFixedThreadPool(servers.size());
            exe.execute(() -> {
                try {
                    sendFractal(fractalFrameColors[indexer.getAndIncrement()]);
                } catch (IOException e) {
                    e.printStackTrace();
                    balcGUI.onException("", e);
                }
            });
            exe.shutdown();
            exe.awaitTermination(5, TimeUnit.HOURS);

            balcGUI.onDisplay(Color.GREEN, "fractal sent to client");
            clientInputStream.close();
        } catch (IOException | InterruptedException io) {
            balcGUI.onException("", io);
        }
    }

    /**
     * sends the fractal images to the client
     * @param fractalFrames - servers, frames, image bytes
     * @throws IOException          - socket error
     * @throws InterruptedException - sleep
     */
    private void sendFractal(byte[][] fractalFrames) throws IOException {
        clientOutputStream.writeObject(fractalFrames);
        clientOutputStream.flush();
    }
}
