package main;

import gui.BalcGUI;

import javax.swing.*;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class Main {

    public static void main(String[] args) {
        // write your code here
        JFrame frame = new JFrame();
        frame.setContentPane(new BalcGUI().getMainPanel());
        frame.pack();
        frame.setBounds(180, 40, 1200, 680);
        //frame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
