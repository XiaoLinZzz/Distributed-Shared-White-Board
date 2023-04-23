package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import WhiteboardClient;

public class Toolbar extends JToolBar {
    private WhiteboardServerInterface server;
    private WhiteboardClient client;
    private JLabel statusLabel;
    private WhiteboardClient.DrawingShape currentShape = WhiteboardClient.DrawingShape.LINE;
    private Color currentColor = Color.BLACK;

    public Toolbar(WhiteboardServerInterface server, WhiteboardClient client) {
        this.server = server;
        this.client = client;
        setFloatable(false);
        setRollover(true);
        setLayout(new BorderLayout());

        statusLabel = new JLabel("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    server.clear();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        buttonPanel.add(clearButton);

        JButton lineButton = new JButton("Line");
        lineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentShape = WhiteboardClient.DrawingShape.LINE;
                setCurrentShapeStatus();
            }
        });
        buttonPanel.add(lineButton);

        JButton circleButton = new JButton("Circle");
        circleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentShape = WhiteboardClient.DrawingShape.CIRCLE;
                setCurrentShapeStatus();
            }
        });
        buttonPanel.add(circleButton);

        JButton ovalButton = new JButton("Oval");
        ovalButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentShape = WhiteboardClient.DrawingShape.OVAL;
                setCurrentShapeStatus();
            }
        });
        buttonPanel.add(ovalButton);

        JButton rectangleButton = new JButton("Rectangle");
        rectangleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentShape = WhiteboardClient.DrawingShape.RECTANGLE;
                setCurrentShapeStatus();
            }
        });
        buttonPanel.add(rectangleButton);

        add(buttonPanel, BorderLayout.CENTER);
    }

    private void setCurrentShapeStatus() {
        statusLabel.setText("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
    }

    private String colorToString(Color color) {
        if (color.equals(Color.BLACK)) return "Black";
        if (color.equals(Color.BLUE)) return "Blue";
        if (color.equals(Color.CYAN)) return "Cyan";
        if (color.equals(Color.DARK_GRAY)) return "Dark Gray";
        if (color.equals(Color.GRAY)) return "Gray";
        if (color.equals(Color.GREEN)) return "Green";
        if (color.equals(Color.LIGHT_GRAY)) return "Light Gray";
        if (color.equals(Color.MAGENTA)) return "Magenta";
        if (color.equals(Color.ORANGE)) return "Orange";
        if (color.equals(Color.PINK)) return "Pink";
        if (color.equals(Color.RED)) return "Red";
        if (color.equals(Color.WHITE)) return "White";
        if (color.equals(Color.YELLOW)) return "Yellow";
        if (color.equals(new Color(128, 0, 128))) return "Purple";
        if (color.equals(new Color(0, 128, 128))) return "Teal";
        if (color.equals(new Color(128, 128, 0))) return "Olive";


        return "RGB(" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ")";
    }
}
