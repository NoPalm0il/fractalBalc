package network;

import gui.GuiUpdate;
import network.shared.BalancerRMI;
import network.shared.ServerRMI;

import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.util.ArrayList;

/**
 * Classe que regista os servidores no {@link ArrayList} do balanceador contendo os stubs
 * dos servidores ({@link ServerRMI}).
 */
public class ServerHandlerRMI implements BalancerRMI {

    private final ArrayList<ServerRMI> servers;
    private final GuiUpdate balcGUI;

    /**
     * Contrutor apenas para inicializar os atributos
     * @param balcGUI recebe os metodos para atualizar o log e output de informacao
     * @param servers recebe a lista dos servidores
     */
    public ServerHandlerRMI(GuiUpdate balcGUI, ArrayList<ServerRMI> servers) {
        this.balcGUI = balcGUI;
        this.servers = servers;
    }

    /**
     * Adiciona à lista {@code this.servers} o servidor conectado
     * @param serverRMI stub do servidor
     * @throws RemoteException
     */
    @Override
    public void serverConnected(ServerRMI serverRMI) throws RemoteException {
        try {
            servers.add(serverRMI);
            balcGUI.onDisplay(Color.GREEN, "Server connected " + RemoteServer.getClientHost());
        } catch (ServerNotActiveException sne) {
            balcGUI.onException("", sne);
        }
    }
}
