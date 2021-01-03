package handlers;
import gui.BalcGUI;
import utils.ImageUtils;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;

public class ServerHandler implements Runnable {

    private final DataInputStream serverInputStream;
    private final DataOutputStream serverOutputStream;
    private String fractalParams;
    private final BalcGUI balcGUI;
    private final byte[] fractalBuffer;
    private int start, end;
    private final Socket socket;

    public ServerHandler(Socket socket, DataInputStream serverInputStream, DataOutputStream serverOutputStream, BalcGUI balcGUI) {
        this.serverInputStream = serverInputStream;
        this.serverOutputStream = serverOutputStream;
        this.balcGUI = balcGUI;
        fractalBuffer = new byte[1024 * 1024];
        this.socket = socket;
    }

    @Override
    public void run() {
        int bytesRead, totalBytes = 0;
        try {
            fractalParams += " " + start + " " + end;
            serverOutputStream.writeUTF(fractalParams);
            serverOutputStream.flush();
            // read bytes
            while((bytesRead = serverInputStream.read(fractalBuffer)) > 0)
                totalBytes += bytesRead;

            balcGUI.onDisplay(Color.YELLOW, "received from server " + totalBytes + " bytes");
        } catch (IOException io) {
            balcGUI.onException("", io);
        } finally {
            balcGUI.onDisplay(Color.YELLOW, "server finished transmission");
        }
    }

    public void setFractalParams(String fractalParams) {
        this.fractalParams = fractalParams;
    }

    public byte[] getFractalBuffer() {
        return fractalBuffer;
    }

    public void setWork(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public Socket getSocket() {
        return socket;
    }

}
