package handlers;
import gui.BalcGUI;

import java.awt.*;
import java.io.*;
import java.net.Socket;

public class ServerHandler implements Runnable{

    private final DataInputStream serverInputStream;
    private final DataOutputStream serverOutputStream;
    private String fractalParams;
    private final BalcGUI balcGUI;
    private byte[] fractalBuffer;
    //private final Socket socket;

    public ServerHandler(DataInputStream serverInputStream, DataOutputStream serverOutputStream, BalcGUI balcGUI) {
        //this.socket = socket;
        this.serverInputStream = serverInputStream;
        this.serverOutputStream = serverOutputStream;
        this.balcGUI = balcGUI;
        fractalBuffer = new byte[1024 * 1024];
    }

    @Override
    public void run() {
        int bytesRead, totalBytes = 0;
        try {
            serverOutputStream.writeUTF(fractalParams);
            serverOutputStream.flush();
            // read bytes
            while((bytesRead = serverInputStream.read(fractalBuffer)) > 0)
                totalBytes += bytesRead;

            balcGUI.onDisplay(Color.YELLOW, "received " + totalBytes + " bytes");
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
}