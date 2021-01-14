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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerRemote extends UnicastRemoteObject implements ServerRMI, Runnable {

    public static int SERVER_PORT = 13337;
    private final GuiUpdate serverGui;
    private final String balAddress;
    private String[] fractalParams;
    private String svAddress;
    private int[] indexes;
    private int totalFrames;

    public ServerRemote(GuiUpdate serverGui, String balAddress) throws RemoteException {
        super(SERVER_PORT);
        this.serverGui = serverGui;
        this.balAddress = balAddress;
    }

    @Override
    public void run() {
        try {
            // Connecting to balancer RMI
            String address = String.format("//%s/%s", balAddress, "bal");
            BalancerRMI stub = (BalancerRMI) Naming.lookup(address);

            // Creating server RMI
            stub.serverConnected(this);

            String host = InetAddress.getLocalHost().getHostAddress();
            LocateRegistry.createRegistry(ServerRemote.SERVER_PORT);
            svAddress = String.format("//%s:%d/%s", host, ServerRemote.SERVER_PORT, "server");
            Naming.rebind(svAddress, this);

            serverGui.onStart();
        } catch (RemoteException | UnknownHostException | MalformedURLException | NotBoundException remoteException) {
            serverGui.onException("", remoteException);
        }
    }

    public void stopServer() {
        try {
            Naming.unbind(svAddress);
            serverGui.onStop();
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            e.printStackTrace();
            serverGui.onException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void setFractalParams(String fractalParams) throws RemoteException {
        this.fractalParams = fractalParams.split(" ");
        serverGui.onDisplay(Color.YELLOW, "Received params");
    }

    @Override
    public byte[][] generateFractal() throws RemoteException {
        try {
            serverGui.onDisplay(Color.YELLOW,"Calculating fractal...");
            Point2D center = new Point2D.Double(Double.parseDouble(fractalParams[0]), Double.parseDouble(fractalParams[1]));
            double zoom = Double.parseDouble(fractalParams[2]);
            int iterations = Integer.parseInt(fractalParams[3]);
            int dimX = Integer.parseInt(fractalParams[4]);
            int dimY = Integer.parseInt(fractalParams[5]);
            int start = 0;

            AtomicInteger ticket = new AtomicInteger(start);
            // e criada uma thread pool com "nCores" threads
            int nCores = Runtime.getRuntime().availableProcessors();
            int inserted = 0;
            // index frames
            byte[][] framesBytes = new byte[indexes.length][1024 * 1024];
            int quarter = totalFrames / 4, half = quarter * 2, thirdQuarter = quarter * 3;

            // foreach frame
            for(int j = 0; j < totalFrames; j++){
                zoom *= 0.85;
                ticket.set(0);
                if(j == indexes[j]) {
                    ExecutorService exe = Executors.newFixedThreadPool(nCores);

                    BufferedImage fractalColor = new BufferedImage(dimX, dimY, BufferedImage.TYPE_INT_RGB);

                    for (int i = 0; i < nCores; i++) {
                        exe.execute(new FractalPixels(center, zoom, iterations, dimX, dimY, dimX, fractalColor, new Mandelbrot(), ticket));
                    }
                    iterations += 20;
                    // obriga o ExecutorService a nao aceitar mais tasks novas e espera que as threads acabem o processo para poder terminar
                    exe.shutdown();
                    exe.awaitTermination(1, TimeUnit.HOURS);
                    framesBytes[inserted++] = ImageUtils.imageToByteArray(fractalColor);

                    if(j == quarter)
                        serverGui.onDisplay(Color.YELLOW, "25%");
                    else if(j == half)
                        serverGui.onDisplay(Color.YELLOW, "50%");
                    else if(j == thirdQuarter)
                        serverGui.onDisplay(Color.YELLOW, "75%");
                }
            }

            //balStub.setRectFractalImg(0, start, ImageUtils.imageToColorArray(fractalColor));
            serverGui.onDisplay(Color.GREEN, "Frames sent");

            return framesBytes;
        } catch (Exception e) {
            serverGui.onException("Error sending frames: ", e);
            e.printStackTrace();
            return new byte[0][];
        }
    }

    @Override
    public void setIndexes(int[] indexes) throws RemoteException {
        this.indexes = indexes;
    }

    @Override
    public void setTotalFrames(int totalFrames) throws RemoteException {
        this.totalFrames = totalFrames;
    }
}
