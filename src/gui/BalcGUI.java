package gui;

import network.shared.BalancerRMI;
import services.Balancer;
import services.ServerRemote;
import utils.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.atomic.AtomicBoolean;

public class BalcGUI {
    private final AtomicBoolean isBalancerRunning;
    private JPanel mainPanel;
    private JTextPane logBalTextPane;
    private JTextField listenAddrTextField;
    private JTextField addressTextField;
    private JButton startBalButton;
    private JButton stopBalButton;
    private JTabbedPane mainTabbedPane;
    private JPanel balancerPanel;
    private JPanel serverPanel;
    private JTextPane logServerTextPane;
    private JButton connectServerButton;
    private JButton disconnectServerButton;
    private Balancer balancer;

    private final BalcGuiUpdate balcGuiUpdate;
    private final ServerGuiUpdate serverGuiUpdate;
    private ServerRemote serverRemote;

    public BalcGUI() {
        isBalancerRunning = new AtomicBoolean(false);
        try {
            addressTextField.setText(InetAddress.getLocalHost().getHostAddress() + ":10011");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        balcGuiUpdate = new BalcGuiUpdate();
        serverGuiUpdate = new ServerGuiUpdate();

        startBalButton.addActionListener(e -> {
            balcGuiUpdate.onStart();
            balancer = new Balancer(balcGuiUpdate, Integer.parseInt(listenAddrTextField.getText()), isBalancerRunning);
            new Thread(balancer).start();
        });

        stopBalButton.addActionListener(e -> {
            setBalancerRunning(false);
            try {
                balancer.getServerSocket().close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            logBalTextPane.setText("");
        });

        connectServerButton.addActionListener(e -> {
            try {
                // Connecting to balancer RMI
                String address = String.format("//%s/%s", addressTextField.getText(), "bal");
                BalancerRMI stub = (BalancerRMI) Naming.lookup(address);

                // Creating server RMI
                serverRemote = new ServerRemote(serverGuiUpdate, stub);
                stub.serverConnected(serverRemote);

                String host = InetAddress.getLocalHost().getHostAddress();
                LocateRegistry.createRegistry(ServerRemote.SERVER_PORT);
                String svAddress = String.format("//%s:%d/%s", host, ServerRemote.SERVER_PORT, "server");
                Naming.rebind(svAddress, serverRemote);

                serverGuiUpdate.onDisplay(Color.GREEN, "Server RMI is running with address " + svAddress);
            } catch (RemoteException | UnknownHostException | MalformedURLException | NotBoundException remoteException) {
                serverGuiUpdate.onException("", remoteException);
            }
        });

        disconnectServerButton.addActionListener(e -> {

        });
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public boolean isBalancerRunning() {
        return isBalancerRunning.get();
    }

    public void setBalancerRunning(boolean serverRunning) {
        isBalancerRunning.set(serverRunning);
    }

    class BalcGuiUpdate implements GuiUpdate {
        @Override
        public void onStart() {
            setBalancerRunning(true);
            startBalButton.setEnabled(false);
            listenAddrTextField.setEnabled(false);
            stopBalButton.setEnabled(true);
        }

        @Override
        public void onStop() {
            setBalancerRunning(false);
            stopBalButton.setEnabled(false);
            startBalButton.setEnabled(true);
            listenAddrTextField.setEnabled(true);
            onDisplay(Color.YELLOW, "Balancer stopped");
        }

        @Override
        public void onException(String message, Exception e) {
            GuiUtils.addException(logBalTextPane, message, e);
        }

        @Override
        public void onDisplay(Color color, String message) {
            GuiUtils.addText(logBalTextPane, message, color);
        }
    }

    public class ServerGuiUpdate implements GuiUpdate {

        @Override
        public void onStart() {
            connectServerButton.setEnabled(false);
            disconnectServerButton.setEnabled(true);
        }

        @Override
        public void onStop() {
            disconnectServerButton.setEnabled(false);
            connectServerButton.setEnabled(true);
        }

        @Override
        public void onException(String message, Exception e) {
            GuiUtils.addException(logServerTextPane, message, e);
        }

        @Override
        public void onDisplay(Color color, String message) {
            GuiUtils.addText(logServerTextPane, message, color);
        }
    }
}
