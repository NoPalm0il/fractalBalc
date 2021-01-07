package services;

import fractal.engine.FractalPixels;
import fractal.models.Mandelbrot;
import gui.GuiUpdate;
import network.shared.BalancerRMI;
import network.shared.ServerRMI;
import utils.ImageUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerRemote extends UnicastRemoteObject implements ServerRMI {

    public static int SERVER_PORT = 13337;
    private final GuiUpdate serverGui;
    private final BalancerRMI balStub;
    private String[] fractalParams;

    public ServerRemote(GuiUpdate serverGui, BalancerRMI stub) throws RemoteException, UnknownHostException {
        super(SERVER_PORT);
        this.serverGui = serverGui;
        balStub = stub;
    }

    @Override
    public void setFractalParams(String fractalParams) throws RemoteException {
        this.fractalParams = fractalParams.split(" ");
        serverGui.onDisplay(Color.GREEN, "Received params");
    }

    @Override
    public void generateFractal() throws RemoteException {
        try {
            Point2D center = new Point2D.Double(Double.parseDouble(fractalParams[0]), Double.parseDouble(fractalParams[1]));
            double zoom = Double.parseDouble(fractalParams[2]);
            int iterations = Integer.parseInt(fractalParams[3]);
            int dimX = Integer.parseInt(fractalParams[4]);
            int dimY = Integer.parseInt(fractalParams[5]);
            int start = Integer.parseInt(fractalParams[6]);
            int end = Integer.parseInt(fractalParams[7]);

            BufferedImage bufferedImage = new BufferedImage(end - start, dimY, BufferedImage.TYPE_INT_RGB);

            AtomicInteger ticket = new AtomicInteger(start);
            // e criada uma thread pool com "nCores" threads
            int nCores = Runtime.getRuntime().availableProcessors();
            ExecutorService exe = Executors.newFixedThreadPool(nCores);

            for (int i = 0; i < nCores; i++) {
                exe.execute(new FractalPixels(
                        center,
                        zoom,
                        iterations,
                        dimX,
                        dimY,
                        bufferedImage,
                        new Mandelbrot(),
                        ticket));
            }
            // obriga o ExecutorService a nao aceitar mais tasks novas e espera que as threads acabem o processo para poder terminar
            exe.shutdown();
            exe.awaitTermination(1, TimeUnit.HOURS);

            balStub.setRectFractalImg(0, start, ImageUtils.imageToColorArray(bufferedImage));
            serverGui.onDisplay(Color.GREEN, "Image sent");
        } catch (Exception e) {
            serverGui.onException("", e);
        }
    }
}
