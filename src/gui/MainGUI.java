package gui;

import services.Balancer;
import services.ServerRemote;
import utils.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainGUI {
    private final AtomicBoolean isBalancerRunning;
    private final BalcGuiUpdate balcGuiUpdate;
    private final ServerGuiUpdate serverGuiUpdate;
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
    private ServerRemote serverRemote;

    public MainGUI() {
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
        });

        connectServerButton.addActionListener(e -> {
            try {
                serverRemote = new ServerRemote(serverGuiUpdate, addressTextField.getText());
                new Thread(serverRemote).start();
            } catch (RemoteException remoteException) {
                remoteException.printStackTrace();
            }
        });

        disconnectServerButton.addActionListener(e -> serverRemote.stopServer());
    }

    public JPanel getMainPanel() {
        return mainPanel;
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

    class ServerGuiUpdate implements GuiUpdate {

        @Override
        public void onStart() {
            onDisplay(Color.GREEN, "Server RMI is running");
            connectServerButton.setEnabled(false);
            disconnectServerButton.setEnabled(true);
        }

        @Override
        public void onStop() {
            disconnectServerButton.setEnabled(false);
            connectServerButton.setEnabled(true);
            onDisplay(Color.YELLOW, "Server stopped");
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
