package network.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface que é implementada pelo servidor, executa os métodos necessários
 * entre balanceador - servidor
 * @see services.ServerRemote
 */
public interface ServerRMI extends Remote {
    /**
     * define os parametros do fratal
     * @param fractalParams ponto, zoom, iterations, dim x and y, frames, tipo fratal
     * @throws RemoteException necessário para o RMI
     */
    void setFractalParams(String fractalParams) throws RemoteException;

    /**
     * o servidor gera o fratal e devolve as frames e os bytes das imagens
     * @return byte[0] -> 1a frame, byte[0][0] -> 1o byte da imagem fratal
     * @throws RemoteException
     */
    byte[][] generateFractal() throws RemoteException;

    /**
     * defines os indexes do servidor, caso haja mais do que 1 servidor
     * é necessário repartir todas as frames para que o trabalho seja dividido
     * @param indexes array de indexes para calcular
     * @throws RemoteException necessário para o RMI
     */
    void setIndexes(int[] indexes) throws RemoteException;

    /**
     * método para verificar se o servidor está ativo
     * @throws RemoteException necessário para o RMI
     */
    void isAlive() throws RemoteException;
}
