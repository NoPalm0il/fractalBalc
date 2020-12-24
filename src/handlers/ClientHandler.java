package handlers;

import gui.BalcGUI;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    private final DataInputStream clientInputStream;
    private final DataOutputStream clientOutputStream;
    private ArrayList<ServerHandler> servers;
    private final BalcGUI balcGUI;

    public ClientHandler(DataInputStream clientInputStream, DataOutputStream clientOutputStream, ArrayList<ServerHandler> servers, BalcGUI balcGUI) {
        this.clientInputStream = clientInputStream;
        this.clientOutputStream = clientOutputStream;
        this.servers = servers;
        this.balcGUI = balcGUI;
    }

    @Override
    public void run() {
        try {
            String rawString = clientInputStream.readUTF();
/*
            String[] fractalParams = rawString.split(" ");
            int dimX = Integer.parseInt(fractalParams[4]);
            int work = dimX / servers.size();
 */
            Thread[] pool = new Thread[servers.size()];
            int i = 0;
            for (ServerHandler server : servers) {
                server.setFractalParams(rawString);
                pool[i] = new Thread(server);
                pool[i].start();
                i++;
            }
            for (Thread thread : pool) {
                thread.join();
            }

            for (ServerHandler server : servers) {
                clientOutputStream.write(server.getFractalBuffer());
                servers.remove(server);
            }

            balcGUI.onDisplay(Color.GREEN, "fractal sent to client");
            clientInputStream.close();
        } catch (IOException | InterruptedException io) {
            balcGUI.onException("", io);
        } finally {
            balcGUI.onDisplay(Color.YELLOW, "client stopped");
        }
    }
}
