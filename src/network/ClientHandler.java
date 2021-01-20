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

/**
 * Classe que trata da conexao do cliente, nesta classe quando o cliente se
 * conecta é feito o pedido dos fratais aos servidores.
 */
public class ClientHandler implements Runnable {

    public static final int FRAME_OFFSET = 1024 * 512;
    private final DataInputStream clientInputStream;
    private final ObjectOutputStream clientOutputStream;
    private final ArrayList<ServerRMI> servers;
    private final GuiUpdate balcGUI;

    /**
     * @param clientInputStream  takes the client input stream
     * @param clientOutputStream takes the client object output stream
     * @param servers            servers connected to the balancer
     * @param balcGUI            gui class to log information
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

            // buffer que irá conter as frames de cada servidor
            byte[][][] fractalFrameImages = new byte[servers.size()][totalFrames][FRAME_OFFSET];

            int totalServers;
            totalServers = checkServers();
           // frames atribuidas a cada servidor para este calcular
            int[][] framesPerServer = assignServerFrameIndexes(totalFrames, totalServers);

            int i = 0;
            for (ServerRMI server : servers) {
                server.setFractalParams(rawString);
                server.setIndexes(framesPerServer[i++]);
            }
            // pede a todos os servidores as frames
            ExecutorService exe = Executors.newFixedThreadPool(totalServers);
            AtomicInteger indexer = new AtomicInteger();
            for (ServerRMI server : servers) {
                exe.execute(() -> {
                    try {
                        fractalFrameImages[indexer.getAndIncrement()] = server.generateFractal();
                    } catch (RemoteException re) {
                        balcGUI.onException("", re);
                    }
                });
            }
            exe.shutdown();
            exe.awaitTermination(1, TimeUnit.HOURS);

            balcGUI.onDisplay(Color.YELLOW, "received frames from servers, sending to client...");
            // buffer que irá contem as frames organizadas e enviar para o cliente
            byte[][] toSend = new byte[totalFrames][FRAME_OFFSET];
            int inserted = 0;

            // organizacao das imagens
            for (int j = 0; j < totalServers; j++) {
                for (int frameIndex : framesPerServer[j]) {
                    toSend[frameIndex] = fractalFrameImages[j][inserted++];
                }
                inserted = 0;
            }
            sendFractal(toSend);
            balcGUI.onDisplay(Color.GREEN, "fractal sent to client");
            clientInputStream.close();
            clientOutputStream.close();
        } catch (IOException | InterruptedException io) {
            balcGUI.onException("", io);
        }
    }

    /**
     * Envia para o cliente as frames/imagens ({@link java.awt.image.BufferedImage}).
     *
     * @param fractalFrames cada frame e os bytes desta
     * @throws IOException socket error
     */
    private void sendFractal(byte[][] fractalFrames) throws IOException {
        clientOutputStream.writeObject(fractalFrames);
        clientOutputStream.flush();
    }

    /**
     * Define as frames com intervalos iguais de acordo com a quantidade
     * de servidores ({@link ServerRMI}) conectados ao balanceador.
     *
     * <p>Uma forma para que haja uma carga de trabalho mais equilibrada.
     *
     * @param totalFrames quantidade de frames que o cliente pediu
     * @param totalServers quantidade de servidores conectados ao balanceador
     * @return matriz que contem os indexes de cada servidor
     */
    private int[][] assignServerFrameIndexes(int totalFrames, int totalServers) {
        int[][] framesPerServer;
        int serverFrames = totalFrames / totalServers;

        if (totalFrames % totalServers != 0)
            // caso seja impar é adicionada uma frame a mais como margem de erro
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

    /**
     * Verifica os servidores conectados, caso um servidor não esteja
     * é gerada a exceção {@link RemoteException} e este é removido.
     * @return quantidade de servidores conectado ao balanceador
     */
    private int checkServers() {
        for (ServerRMI server : servers) {
            try {
                server.isAlive();
            } catch (RemoteException re) {
                balcGUI.onException("removing server from table", re);
                servers.remove(server);
            }
        }
        balcGUI.onDisplay(Color.YELLOW, "servers connected: " + servers.size());
        return servers.size();
    }
}
