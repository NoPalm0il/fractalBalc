package network.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BalancerRMI extends Remote {
    void serverConnected(ServerRMI serverRMI) throws RemoteException;
}
