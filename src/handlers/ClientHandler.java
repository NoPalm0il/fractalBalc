package handlers;

import gui.BalcGUI;
import utils.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

public class ClientHandler implements Runnable {

    private final DataInputStream clientInputStream;
    private final DataOutputStream clientOutputStream;
    private final CopyOnWriteArrayList<ServerHandler> servers;
    private final BalcGUI balcGUI;

    public ClientHandler(DataInputStream clientInputStream, DataOutputStream clientOutputStream, CopyOnWriteArrayList<ServerHandler> servers, BalcGUI balcGUI) {
        this.clientInputStream = clientInputStream;
        this.clientOutputStream = clientOutputStream;
        this.servers = servers;
        this.balcGUI = balcGUI;
    }

    @Override
    public void run() {
        try {
            String rawString = clientInputStream.readUTF();

            String[] fractalParams = rawString.split(" ");
            int dimX = Integer.parseInt(fractalParams[4]);
            int dimY = Integer.parseInt(fractalParams[5]);
            int work = dimX / servers.size();

            ExecutorService exe = Executors.newFixedThreadPool(servers.size());
            int i = 0;
            for (ServerHandler server : servers) {
                server.setFractalParams(rawString);
                server.setWork(i * work, (i + 1) * work);
                exe.execute(server);
                i++;
            }
            exe.shutdown();
            exe.awaitTermination(1, TimeUnit.HOURS);

            // TODO: After servers finish their work, it needs to assemble the buffered image
            ArrayList<BufferedImage> incomes = new ArrayList<>();
            for (ServerHandler server : servers) {
                BufferedImage img = ImageUtils.byteArrayToImage(server.getFractalBuffer());
                if(img != null)
                    incomes.add(img);
                servers.remove(server);
            }

            BufferedImage constructedImg = new BufferedImage(dimX, dimY, BufferedImage.TYPE_INT_RGB);
            i = 0;
            for (BufferedImage img : incomes) {
                //constructedImg.getRaster().setDataElements(work * i ,0, img);
                constructedImg = img;
                i++;
            }

            clientOutputStream.write(ImageUtils.imageToByteArray(constructedImg));

            balcGUI.onDisplay(Color.GREEN, "fractal sent to client");
            clientInputStream.close();
        } catch (IOException | InterruptedException io) {
            balcGUI.onException("", io);
        } finally {
            balcGUI.onDisplay(Color.YELLOW, "client stopped");
        }
    }
}
