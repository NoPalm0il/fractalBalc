package sockets;

import gui.BalcGUI;
import handlers.ClientHandler;
import handlers.ServerHandler;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class Balancer implements Runnable {

    private final BalcGUI balcGUI;
    private ServerSocket serverSocket;
    private final ArrayList<ServerHandler> servers;

    public Balancer(BalcGUI balcGUI) {
        this.balcGUI = balcGUI;
        servers = new ArrayList<>();
    }

    @Override
    public void run() {
        balcGUI.onStart();

        try {
            serverSocket = new ServerSocket(balcGUI.getListenPort());
            balcGUI.onDisplay(Color.GREEN, "Balancer listening on port " + serverSocket.getLocalPort());

            while (balcGUI.isServerRunning()) {
                try {
                    Socket socket = serverSocket.accept();
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                    balcGUI.onDisplay(Color.GREEN, "connection form " + socket.getInetAddress() + ":" + socket.getPort());

                    switch (dis.readChar()) {
                        case 'c':
                            balcGUI.onDisplay(Color.GREEN, "Client connected");

                            if(servers.size() < 1) {
                                balcGUI.onDisplay(Color.RED, "No servers connected, closing socket");
                                socket.close();
                                break;
                            } else {
                                ClientHandler client = new ClientHandler(dis, dos, servers, balcGUI);
                                new Thread(client).start();
                            }
                            break;
                        case 's':
                            balcGUI.onDisplay(Color.GREEN, "Server connected");

                            ServerHandler sh = new ServerHandler(dis, dos, balcGUI);
                            servers.add(sh);
                            break;
                        default:
                            socket.close();
                            throw new IOException("Unknown socket type from" + socket);
                    }
                } catch (SocketException se) {
                    balcGUI.onDisplay(Color.RED, "ServerSocket stopped accepting");
                } catch (EOFException eo) {
                    balcGUI.onException("socket input stream stopped", eo);
                } catch (IOException io) {
                    balcGUI.onException(io.getMessage(), io);
                }
            }
        } catch (IOException e) {
            balcGUI.onException(e.getMessage(), e);
        }

        balcGUI.onStop();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }
}
