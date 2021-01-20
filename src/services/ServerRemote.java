package services;

import fractal.Fractal;
import fractal.engine.FractalPixels;
import fractal.models.BurningShip;
import fractal.models.Julia;
import fractal.models.Julia2;
import fractal.models.Mandelbrot;
import gui.GuiUpdate;
import network.shared.BalancerRMI;
import network.shared.ServerRMI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Esta é a classe dos serviços do servidor, cria o RMI
 * recebe os parametros do fratal e calcula-o, e devolve numa matriz de bytes
 */
public class ServerRemote extends UnicastRemoteObject implements ServerRMI, Runnable {

    public static int SERVER_PORT = 13337;
    private final GuiUpdate serverGui;
    private final String balAddress;
    private String[] fractalParams;
    private String svAddress;
    private int[] indexes;
    private int totalFrames;

    /**
     * @param serverGui  recebe os metodos para atualizar o log e output de informacao
     * @param balAddress balancer address
     * @throws RemoteException by the super constructor
     * @see UnicastRemoteObject
     */
    public ServerRemote(GuiUpdate serverGui, String balAddress) throws RemoteException {
        super(SERVER_PORT);
        this.serverGui = serverGui;
        this.balAddress = balAddress;
    }

    /**
     * Conecta-se ao RMI do balanceador e inicia o servico RMI do servidor
     */
    @Override
    public void run() {
        try {
            // Connecting to balancer RMI
            String address = String.format("//%s/%s", balAddress, "bal");
            BalancerRMI stub = (BalancerRMI) Naming.lookup(address);

            // Sends this class to balancer
            stub.serverConnected(this);

            String host = InetAddress.getLocalHost().getHostAddress();
            // create server RMI
            LocateRegistry.createRegistry(ServerRemote.SERVER_PORT);
            svAddress = String.format("//%s:%d/%s", host, ServerRemote.SERVER_PORT, "server");
            Naming.rebind(svAddress, this);

            // update GUI, logs info
            serverGui.onStart();
        } catch (RemoteException | UnknownHostException | MalformedURLException | NotBoundException remoteException) {
            serverGui.onException("", remoteException);
        }
    }

    /**
     * Unbind do registo
     * @see Naming
     */
    public void stopServer() {
        try {
            Naming.unbind(svAddress);
        } catch (RemoteException | NotBoundException | MalformedURLException e) {
            e.printStackTrace();
            serverGui.onException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Recebe a string e separa cada propriedade pelo espaco.
     * @param fractalParams ponto, zoom, iterations, dim x and y, frames, tipo fratal
     * @throws RemoteException
     */
    @Override
    public void setFractalParams(String fractalParams) throws RemoteException {
        this.fractalParams = fractalParams.split(" ");
        serverGui.onDisplay(Color.YELLOW, "Received params");
    }

    /**
     * Gera o fratal e devolve as frames e os bytes de cada frame
     * @return byte[frame][byte]
     * @throws RemoteException
     */
    @Override
    public byte[][] generateFractal() throws RemoteException {
        try {
            serverGui.onDisplay(Color.YELLOW, "Calculating...");
            Point2D center = new Point2D.Double(Double.parseDouble(fractalParams[0]), Double.parseDouble(fractalParams[1]));
            double zoom = Double.parseDouble(fractalParams[2]);
            int iterations = Integer.parseInt(fractalParams[3]);
            int dimX = Integer.parseInt(fractalParams[4]);
            int dimY = Integer.parseInt(fractalParams[5]);
            totalFrames = Integer.parseInt(fractalParams[6]);
            serverGui.onDisplay(Color.YELLOW, "total frames: " + totalFrames);
            String frChoice = fractalParams[7];
            Fractal fractalToRender;
            switch (frChoice) {
                case "b":
                    fractalToRender = new BurningShip();
                    break;
                case "j1":
                    fractalToRender = new Julia();
                    break;
                case "j2":
                    fractalToRender = new Julia2();
                    break;
                default:
                    fractalToRender = new Mandelbrot();
            }

            AtomicInteger ticket = new AtomicInteger();
            // e criada uma thread pool com "nCores" threads
            int nCores = Runtime.getRuntime().availableProcessors();
            int inserted = 0;
            // index frames
            byte[][] framesBytes = new byte[indexes.length][1024 * 512];
            int quarter = indexes.length / 4, half = indexes.length / 2, thirdQuarter = (indexes.length * 3) / 4;
            ArrayList<Integer> frames = new ArrayList<>();

            for (int index : indexes)
                frames.add(index);

            // foreach frame
            for (int i = 0; i < totalFrames; i++) {
                if (frames.contains(i)) {
                    ticket.set(0);
                    ExecutorService exe = Executors.newFixedThreadPool(nCores);

                    BufferedImage fractalImage = new BufferedImage(dimX, dimY, BufferedImage.TYPE_INT_RGB);

                    for (int k = 0; k < nCores; k++) {
                        exe.execute(new FractalPixels(center, zoom, iterations, dimX, dimY, dimX, fractalImage, fractalToRender, ticket));
                    }
                    // obriga o ExecutorService a nao aceitar mais tasks novas e espera que as threads acabem o processo para poder terminar
                    exe.shutdown();
                    exe.awaitTermination(1, TimeUnit.HOURS);
                    framesBytes[inserted++] = imageToByteArray(fractalImage);

                    if (inserted == quarter)
                        serverGui.onDisplay(Color.YELLOW, "25%");
                    else if (inserted == half)
                        serverGui.onDisplay(Color.YELLOW, "50%");
                    else if (inserted == thirdQuarter)
                        serverGui.onDisplay(Color.YELLOW, "75%");
                }
                iterations += 20;
                zoom *= 0.88;
            }
            serverGui.onDisplay(Color.GREEN, "Frames sent");

            return framesBytes;
        } catch (Exception e) {
            serverGui.onException("Error sending frames: ", e);
            e.printStackTrace();
            return new byte[0][];
        }
    }

    /**
     * Define os indexes deste servidor.
     * @param indexes array de indexes para calcular
     * @throws RemoteException
     */
    @Override
    public void setIndexes(int[] indexes) throws RemoteException {
        this.indexes = indexes;
        serverGui.onDisplay(Color.YELLOW, "frames to render: " + indexes.length);
    }

    /**
     * Verifica apenas se está ativo.
     * @throws RemoteException
     */
    @Override
    public void isAlive() throws RemoteException {
    }

    /**
     * Traduz a imagem para bytes
     * @param image recebe a imagem para ser posta em bytes
     * @return image bytes
     */
    private byte[] imageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        baos.flush();
        return baos.toByteArray();
    }
}
