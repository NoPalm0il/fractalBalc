package gui;

import sockets.Balancer;
import utils.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class BalcGUI implements GuiUpdate {
    private JPanel mainPanel;
    private JTextPane logTextPane;
    private JTextField listenPortTextField;
    private JButton startButton;
    private JButton stopButton;

    private final AtomicBoolean isServerRunning;
    private final Balancer balancer;

    public BalcGUI(){
        isServerRunning = new AtomicBoolean(false);
        balancer = new Balancer(this);

        startButton.addActionListener(e -> {
            Thread td = new Thread(balancer);
            td.start();
        });
        stopButton.addActionListener(e -> {
            setServerRunning(false);
            try {
                balancer.getServerSocket().close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
    }

    @Override
    public void onStart() {
        setServerRunning(true);
        startButton.setEnabled(false);
        listenPortTextField.setEnabled(false);
        stopButton.setEnabled(true);
    }

    @Override
    public void onStop() {
        setServerRunning(false);
        stopButton.setEnabled(false);
        startButton.setEnabled(true);
        listenPortTextField.setEnabled(true);
        onDisplay(Color.YELLOW, "Server stopped");
    }

    @Override
    public void onException(String message, Exception e) {
        GuiUtils.addException(logTextPane, message, e);
    }

    @Override
    public void onDisplay(Color color, String message) {
        GuiUtils.addText(logTextPane, message, color);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public boolean isServerRunning() {
        return isServerRunning.get();
    }

    public void setServerRunning(boolean serverRunning) {
        isServerRunning.set(serverRunning);
    }

    public int getListenPort() {
        return Integer.parseInt(listenPortTextField.getText());
    }
}
