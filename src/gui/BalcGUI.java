package gui;

import balancer.Balancer;
import utils.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class BalcGUI implements GuiUpdate {
    private JPanel mainPanel;
    private JTextPane logTextPane;
    private JTextField listenAddrTextField;
    private JButton startButton;
    private JButton stopButton;

    private final AtomicBoolean isServerRunning;
    private Balancer balancer;

    public BalcGUI(){
        isServerRunning = new AtomicBoolean(false);

        startButton.addActionListener(e -> {
            balancer = new Balancer(this);
            new Thread(balancer).start();
        });
        stopButton.addActionListener(e -> {
            setServerRunning(false);
            try {
                balancer.getServerSocket().close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            logTextPane.setText("");
        });
    }

    @Override
    public void onStart() {
        setServerRunning(true);
        startButton.setEnabled(false);
        listenAddrTextField.setEnabled(false);
        stopButton.setEnabled(true);
    }

    @Override
    public void onStop() {
        setServerRunning(false);
        stopButton.setEnabled(false);
        startButton.setEnabled(true);
        listenAddrTextField.setEnabled(true);
        onDisplay(Color.YELLOW, "Balancer stopped");
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
        return Integer.parseInt(listenAddrTextField.getText());
    }
}
