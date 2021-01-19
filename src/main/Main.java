package main;

import gui.MainGUI;

import javax.swing.*;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

/**
 * @author Tiago Murteira   n21087
 * @author Joao Ramos       n21397
 */
public class Main {
    public static void main(String[] args) {
        // Inicia o GUI
        JFrame frame = new JFrame("Fractal Balancer & Server");
        frame.setContentPane(new MainGUI().getMainPanel());
        frame.pack();
        frame.setBounds(180, 40, 1200, 680);
        //frame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
