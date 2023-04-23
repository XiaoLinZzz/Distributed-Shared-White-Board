package client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import javax.swing.JPanel;

import WhiteboardClient;

public class DrawingPanel extends JPanel {
    private WhiteboardServerInterface server;
    private WhiteboardClient client;
    private BufferedImage image;
    private Point startPoint;
    private Point endPoint;
    private Color currentColor = Color.BLACK;
    private WhiteboardClient.DrawingShape currentShape = WhiteboardClient.DrawingShape.LINE;

    public DrawingPanel(WhiteboardServerInterface server, WhiteboardClient client) {
        this.server = server;
        this.client = client;
        setPreferredSize(new Dimension(600, 400));
        setBackground(Color.WHITE);
        image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                endPoint = e.getPoint();
                try {
                    server.drawShape(currentShape, startPoint, endPoint, currentColor);
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);
    }

    public void drawShape(WhiteboardClient.DrawingShape shape, Point start, Point end, Color color) {
        Graphics g = image.getGraphics();
        g.setColor(color);

        switch (shape) {
            case LINE:
                g.drawLine(start.x, start.y, end.x, end.y);
                break;
            case CIRCLE:
                int diameter = Math.max(Math.abs(end.x - start.x), Math.abs(end.y - start.y));
                g.drawOval(start.x, start.y, diameter, diameter);
                break;
            case OVAL:
                g.drawOval(start.x, start.y, Math.abs(end.x - start.x), Math.abs(end.y - start.y));
                break;
            case RECTANGLE:
                g.drawRect(start.x, start.y, Math.abs(end.x - start.x), Math.abs(end.y - start.y));
                break;
            default:
                break;
        }
    }

    public void clear() {
        image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        repaint();
    }

    public void setCurrentColor(Color color) {
        currentColor = color;
    }

    public void setCurrentShape(WhiteboardClient.DrawingShape shape) {
        currentShape = shape;
    }
}
