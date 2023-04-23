package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import WhiteboardClient;

public class ColorChooser extends JToolBar {
    private WhiteboardServerInterface server;
    private WhiteboardClient client;
    private Color currentColor = Color.BLACK;

    public ColorChooser(WhiteboardServerInterface server, WhiteboardClient client) {
        this.server = server;
        this.client = client;
        setFloatable(false);
        setRollover(true);

        JPanel colorPanel = new JPanel();
        Color Purple = new Color(128, 0, 128);
        Color Teal = new Color(0, 128, 128);
        Color Olive = new Color(128, 128, 0);
        Color[] colors = {Color.BLACK, Color.BLUE, Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.WHITE, Color.YELLOW, Purple, Teal, Olive};

        for (Color color : colors) {
            JButton colorButton = new JButton();
            colorButton.setOpaque(true);
            colorButton.setBackground(color);
            colorButton.setPreferredSize(new Dimension(20, 20));

            colorButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

            colorButton.addActionListener(e -> {
                try {
                    setCurrentColor(color);
                    setCurrentColorStatus(color);

                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
            });
            colorPanel.add(colorButton);
        }

        add(colorPanel);
    }

    private void setCurrentColor(Color color) {
        currentColor = color;
    }

    private void setCurrentColorStatus(Color color) {
        client.setCurrentColorStatus(color);
    }
}
