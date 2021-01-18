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

/**
 * Classe que contem os serviços do balanceador.
 *
 * <p>Comunica com os servidores atraves de RMI e com o cliente
 * atraves de uma socket.
 */
public class Balancer implements Runnable {

    public static final int BALANCER_PORT = 10011;
    private final GuiUpdate balcGUI;
    private final ArrayList<ServerRMI> servers;
    private final int socketPort;
    private final AtomicBoolean isServerRunning;
    private ServerSocket balancerSocket;
    private ServerHandlerRMI serverHandlerRMI;

    /**
     * Construtor do balanceador, apenas inicializa os atributos
     * @param balcGUI class para inserir a informacao (log)
     * @param socketPort porta para que seja criada a {@link ServerSocket}
     * @param isRunning determina se está ou nao a aceitar conexoes do Cliente
     */
    public Balancer(GuiUpdate balcGUI, int socketPort, AtomicBoolean isRunning) {
        this.balcGUI = balcGUI;
        this.socketPort = socketPort;
        this.isServerRunning = isRunning;
        servers = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            balancerSocket = new ServerSocket(socketPort);
            balcGUI.onDisplay(Color.GREEN, "Balancer socket listening on port " + balancerSocket.getLocalPort());

            String address = String.format("//%s:%d/%s", InetAddress.getLocalHost().getHostAddress(), BALANCER_PORT, "bal");
            BalancerRMI stub = null;
            startBalancerRMI(stub, address);

            while (isServerRunning.get()) {
                try {
                    Socket socket = balancerSocket.accept();
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
     * Comeca o RMI do balanceador {@link BalancerRMI}, exporta a interface desta classe.
     * @param stub é o objeto a ser exportado da interface, é necessário ter a atencao
     * para que o garbage collector não limpe o objeto
     * @param address balancer RMI address
     */
    private void startBalancerRMI(BalancerRMI stub, String address) throws RemoteException, UnknownHostException, MalformedURLException {
        serverHandlerRMI = new ServerHandlerRMI(balcGUI, servers);
        stub = (BalancerRMI) UnicastRemoteObject.exportObject(serverHandlerRMI, BALANCER_PORT);
        LocateRegistry.createRegistry(BALANCER_PORT);

        Naming.rebind(address, stub);

        balcGUI.onDisplay(Color.GREEN, "BalancerRMI on: " + address);
    }

    public ServerSocket getBalancerSocket() {
        return balancerSocket;
    }
}
