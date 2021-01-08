package network.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerRMI extends Remote {
    void setFractalParams(String fractalParams) throws RemoteException;

    int[][] generateFractal() throws RemoteException;
}
