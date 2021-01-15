package network;

import gui.GuiUpdate;
import network.shared.ServerRMI;
import services.ServerRemote;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.rmi.RemoteException;
import java.util.*;
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
            int totalFrames = Integer.parseInt(fractalParams[6]);

            byte[][][] fractalFrameImages = new byte[servers.size()][totalFrames][1024 * 1024];

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
                for (int j = 0; j < servers.size(); j++)
                    for (int i = 0; i < serverFrames; i++)
                        framesPerServer[j][i] = i;

            ExecutorService exe = Executors.newFixedThreadPool(servers.size());
            HashMap<ServerRMI, int[]> framesPerServerMap = new HashMap<>();

            int i = 0;
            for (ServerRMI server : servers) {
                server.setFractalParams(rawString);
                server.setIndexes(framesPerServer[i]);
                server.setTotalFrames(totalFrames);
                for (int j = 0; j < framesPerServer[i].length; j++) {
                    framesPerServerMap.put(server, framesPerServer[i]);
                }
                i++;
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

            byte[][] toSend = new byte[totalFrames][1024 * 1024];
            int inserted = 0;

            for (int j = 0; j < servers.size(); j++) {
                for (int frameIndex : framesPerServerMap.get(servers.get(j))) {
                    toSend[frameIndex] = fractalFrameImages[j][inserted++];
                }
                inserted = 0;
            }
            // todo: isto tá fdd, com 2 pcs n envia todas as frames ou elas ficam fddas
            sendFractal(toSend);
            balcGUI.onDisplay(Color.GREEN, "fractal sent to client");
            clientInputStream.close();
        } catch (IOException | InterruptedException io) {
            balcGUI.onException("", io);
        }
    }

    /**
     * sends the fractal images to the client
     * @param fractalFrames frames, image bytes
     * @throws IOException  socket error
     */
    private void sendFractal(byte[][] fractalFrames) throws IOException {
        clientOutputStream.writeObject(fractalFrames);
        clientOutputStream.flush();
    }
}
