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
    public static final int FRAME_OFFSET = 1024 * 512;

    /**
     *
     * @param clientInputStream takes the client input stream
     * @param clientOutputStream takes the client object output stream
     * @param servers servers connected to the balancer
     * @param balcGUI gui class to log information
     */
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
            int totalFrames = Integer.parseInt(fractalParams[6]);

            // buffer that contains all frames
            byte[][][] fractalFrameImages = new byte[servers.size()][totalFrames][FRAME_OFFSET];

            int totalServers = servers.size();

            int[][] framesPerServer = assignServerFrameIndexes(totalFrames, totalServers);

            totalServers = checkServers();

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
                        fractalFrameImages[indexer.getAndIncrement()] = server.generateFractal();
                    } catch (RemoteException re) {
                        balcGUI.onException("", re);
                    }
                }
            });
            exe.shutdown();
            exe.awaitTermination(1, TimeUnit.HOURS);

            balcGUI.onDisplay(Color.GREEN, "received frames from servers, sending to client...");

            byte[][] toSend = new byte[totalFrames][FRAME_OFFSET];
            int inserted = 0;

            for (int j = 0; j < totalServers; j++) {
                for (int frameIndex : framesPerServer[j]) {
                    toSend[frameIndex] = fractalFrameImages[j][inserted++];
                }
                inserted = 0;
            }
            sendFractal(toSend);
            balcGUI.onDisplay(Color.GREEN, "fractal sent to client");
            clientInputStream.close();
        } catch (IOException | InterruptedException io) {
            balcGUI.onException("", io);
        }
    }

    /**
     * sends the fractal images to the client
     *
     * @param fractalFrames frames containing the fractal image bytes
     * @throws IOException socket error
     */
    private void sendFractal(byte[][] fractalFrames) throws IOException {
        clientOutputStream.writeObject(fractalFrames);
        clientOutputStream.flush();
    }

    private int[][] assignServerFrameIndexes(int totalFrames, int totalServers) {
        int[][] framesPerServer;
        int serverFrames = totalFrames / totalServers;

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
            for (int j = 0; j < servers.size(); j++)
                for (int i = 0; i < serverFrames; i++)
                    framesPerServer[j][i] = i;

        return framesPerServer;
    }

    private int checkServers() {
        for (ServerRMI server : servers) {
            try {
                server.isAlive();
            } catch (RemoteException re) {
                balcGUI.onException("removing server from table", re);
                servers.remove(server);
            }
        }
        return servers.size();
    }
}
