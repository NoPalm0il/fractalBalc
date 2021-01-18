package network.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface para ser utilizada no RMI do balanceador, comunicacao entre
 * servidor balanceador.
 * @see services.Balancer
 */
public interface BalancerRMI extends Remote {
    /**
     * Metodo utilizado para quando o servidor se conectar ao balanceador este envia o próprio stub
     * <p>Assim é possível para o balanceador gerir os servidores
     * @param serverRMI stub do servidor
     * @throws RemoteException
     */
    void serverConnected(ServerRMI serverRMI) throws RemoteException;
}
