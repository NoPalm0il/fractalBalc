package balancer;

import gui.BalcGUI;
import network.shared.BalancerRMI;
import network.ClientHandler;
import network.shared.ServerRMI;
import utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CopyOnWriteArrayList;

public class Balancer implements Runnable, BalancerRMI {

    private final BalcGUI balcGUI;
    private ServerSocket serverSocket;
    private final CopyOnWriteArrayList<ServerRMI> servers;
    private BufferedImage fractalImage;

    public Balancer(BalcGUI balcGUI) {
        this.balcGUI = balcGUI;
        servers = new CopyOnWriteArrayList<>();
    }

    @Override
    public void run() {
        balcGUI.onStart();

        try {
            serverSocket = new ServerSocket(balcGUI.getListenPort());
            balcGUI.onDisplay(Color.GREEN, "Balancer socket listening on port " + serverSocket.getLocalPort());

            BalancerRMI stub = (BalancerRMI) UnicastRemoteObject.exportObject(this, 13337);
            Registry registry = LocateRegistry.createRegistry(13337);
            registry.bind("BalancerRMI", stub);
            balcGUI.onDisplay(Color.GREEN, "BalancerRMI listening on port 13337");

            while (balcGUI.isServerRunning()) {
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
        } catch (IOException | AlreadyBoundException e) {
            balcGUI.onException(e.getMessage(), e);
        }

        balcGUI.onStop();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    @Override
    public void serverConnected(ServerRMI serverRMI) throws RemoteException {
        servers.add(serverRMI);
    }

    @Override
    public void setRectFractalImg(int x, int y, int[][] colorBuffer) throws RemoteException {
        ImageUtils.paintBufferedImage(x, y, colorBuffer, fractalImage);
    }
}
