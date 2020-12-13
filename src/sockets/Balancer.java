package sockets;

import gui.BalcGUI;

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Balancer implements Runnable {

    private final BalcGUI balcGUI;
    private ServerSocket serverSocket;

    public Balancer(BalcGUI balcGUI) {
        this.balcGUI = balcGUI;
    }

    @Override
    public void run() {
        balcGUI.onStart();

        try {
            serverSocket = new ServerSocket(balcGUI.getListenPort());

            balcGUI.onDisplay(Color.GREEN, "Server listening on port " + serverSocket.getLocalPort());

            while (balcGUI.isServerRunning()) {
                try {
                    Socket socket = serverSocket.accept();

                    balcGUI.onDisplay(Color.GREEN, "connection form " + socket.getInetAddress() + ":" + socket.getPort());

                    ObjectInputStream isr = new ObjectInputStream(socket.getInputStream());

                    switch (isr.readChar()) {
                        case 'c':
                            balcGUI.onDisplay(Color.GREEN, "Client socket connected" + socket);
                            break;
                        case 's':
                            balcGUI.onDisplay(Color.GREEN, "Server socket connected" + socket);
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
