package handlers;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerRMI extends Remote {
    void sendPortionFractalImg(int x, int y, int[][] colorBuffer) throws RemoteException;
}
