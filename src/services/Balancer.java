package services;

import gui.GuiUpdate;
import network.ClientHandler;
import network.ServerHandlerRMI;
import network.shared.BalancerRMI;
import network.shared.ServerRMI;

import java.awt.*;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Balancer implements Runnable {

    public static final int BALANCER_PORT = 10011;
    private final GuiUpdate balcGUI;
    private final ArrayList<ServerRMI> servers;
    private final int socketPort;
    private final AtomicBoolean isServerRunning;
    private ServerSocket serverSocket;
    private ServerHandlerRMI serverHandlerRMI;

    public Balancer(GuiUpdate balcGUI, int socketPort, AtomicBoolean isRunning) {
        this.balcGUI = balcGUI;
        this.socketPort = socketPort;
        this.isServerRunning = isRunning;
        servers = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(socketPort);
            balcGUI.onDisplay(Color.GREEN, "Balancer socket listening on port " + serverSocket.getLocalPort());

            String address = String.format("//%s:%d/%s", InetAddress.getLocalHost().getHostAddress(), BALANCER_PORT, "bal");
            BalancerRMI stub = null;
            startBalancerRMI(stub, address);

            while (isServerRunning.get()) {
                try {
                    Socket socket = serverSocket.accept();
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    ObjectOutputStream dos = new ObjectOutputStream(socket.getOutputStream());

                    balcGUI.onDisplay(Color.GREEN, "connection form " + socket.getInetAddress() + ":" + socket.getPort());

                    if (servers.size() < 1) {
                        balcGUI.onDisplay(Color.RED, "No servers connected, closing socket");
                        socket.close();
                    } else {
                        ClientHandler client = new ClientHandler(dis, dos, servers, balcGUI);
                        Thread td = new Thread(client);
                        td.start();
                        td.join();
                    }

                } catch (SocketException se) {
                    balcGUI.onDisplay(Color.RED, "ServerSocket stopped accepting");
                } catch (EOFException eo) {
                    balcGUI.onException("socket input stream stopped", eo);
                } catch (IOException io) {
                    balcGUI.onException(io.getMessage(), io);
                } catch (InterruptedException e) {
                    balcGUI.onException("thread error", e);
                }
            }
            Naming.unbind(address);
        } catch (IOException | NotBoundException e) {
            balcGUI.onException(e.getMessage(), e);
        }

        balcGUI.onStop();
    }

    /**
     * method to start RMI
     *
     * @param stub    is the interface object, be carefully with the garbage collector
     * @param address balancer RMI address
     * @throws RemoteException
     * @throws UnknownHostException
     * @throws MalformedURLException
     */
    private void startBalancerRMI(BalancerRMI stub, String address) throws RemoteException, UnknownHostException, MalformedURLException {
        serverHandlerRMI = new ServerHandlerRMI(balcGUI, servers);
        stub = (BalancerRMI) UnicastRemoteObject.exportObject(serverHandlerRMI, BALANCER_PORT);
        LocateRegistry.createRegistry(BALANCER_PORT);

        Naming.rebind(address, stub);

        balcGUI.onDisplay(Color.GREEN, "BalancerRMI on: " + address);
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }
}
