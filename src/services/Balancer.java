package services;

import gui.GuiUpdate;
import network.ClientHandler;
import network.ServerHandlerRMI;
import network.shared.BalancerRMI;
import network.shared.ServerRMI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Balancer implements Runnable {

    public static final int BALANCER_PORT = 10011;
    private final GuiUpdate balcGUI;
    private final CopyOnWriteArrayList<ServerRMI> servers;
    private final int socketPort;
    private ServerSocket serverSocket;
    private BufferedImage fractalImage;
    private final AtomicBoolean isServerRunning;
    private BalancerRMI stub;
    private ServerHandlerRMI serverHandlerRMI;

    public Balancer(GuiUpdate balcGUI, int socketPort, AtomicBoolean isRunning) {
        this.balcGUI = balcGUI;
        this.socketPort = socketPort;
        this.isServerRunning = isRunning;
        servers = new CopyOnWriteArrayList<>();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(socketPort);
            balcGUI.onDisplay(Color.GREEN, "Balancer socket listening on port " + serverSocket.getLocalPort());

            serverHandlerRMI = new ServerHandlerRMI(balcGUI, fractalImage, servers);
            stub = (BalancerRMI) UnicastRemoteObject.exportObject(serverHandlerRMI, BALANCER_PORT);
            LocateRegistry.createRegistry(BALANCER_PORT);
            String address = String.format("//%s:%d/%s", InetAddress.getLocalHost().getHostAddress(), BALANCER_PORT, "bal");
            Naming.rebind(address, stub);

            balcGUI.onDisplay(Color.GREEN, "BalancerRMI on: " + address);
            while (isServerRunning.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                    balcGUI.onDisplay(Color.GREEN, "connection form " + socket.getInetAddress() + ":" + socket.getPort());

                    if (dis.readChar() == 'c') {
                        balcGUI.onDisplay(Color.GREEN, "Client connected");

                        if (servers.size() < 1) {
                            balcGUI.onDisplay(Color.RED, "No servers connected, closing socket");
                            socket.close();
                        } else {
                            ClientHandler client = new ClientHandler(dis, dos, servers, balcGUI, fractalImage);
                            new Thread(client).start();
                        }
                    } else {
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
