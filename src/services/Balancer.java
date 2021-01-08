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
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Balancer implements Runnable {

    public static final int BALANCER_PORT = 10011;
    private final GuiUpdate balcGUI;
    private final CopyOnWriteArrayList<ServerRMI> servers;
    private final int socketPort;
    private final AtomicBoolean isServerRunning;
    private ServerSocket serverSocket;
    private BufferedImage fractalImage;
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

            String address = String.format("//%s:%d/%s", InetAddress.getLocalHost().getHostAddress(), BALANCER_PORT, "bal");
            Registry balancerReg = startBalancerRMI(address);

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
            balancerReg.unbind(address);
        } catch (IOException | NotBoundException e) {
            balcGUI.onException(e.getMessage(), e);
        }

        balcGUI.onStop();
    }

    private Registry startBalancerRMI(String address) throws RemoteException, UnknownHostException, MalformedURLException {
        serverHandlerRMI = new ServerHandlerRMI(balcGUI, fractalImage, servers);
        stub = (BalancerRMI) UnicastRemoteObject.exportObject(serverHandlerRMI, BALANCER_PORT);
        Registry balancerReg = LocateRegistry.createRegistry(BALANCER_PORT);

        Naming.rebind(address, stub);

        balcGUI.onDisplay(Color.GREEN, "BalancerRMI on: " + address);

        return balancerReg;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }
}
