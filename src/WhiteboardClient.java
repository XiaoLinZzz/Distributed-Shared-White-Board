import java.awt.Color;
import java.awt.Shape;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import javax.swing.*;
import java.util.ArrayList;


public class WhiteboardClient extends UnicastRemoteObject implements WhiteboardClientInterface {
    private JFrame frame;
    private JPanel panel;
    private BufferedImage image;
    private Graphics2D graphics;
    private WhiteboardServerInterface server;
    private int startX, startY;
    private Shape tempShape = null;
    private boolean drawing = false;
    private DrawingShape currentShape = DrawingShape.LINE;

    public enum DrawingShape {
        LINE,
        CIRCLE,
        OVAL,
        RECTANGLE;
    }

    public WhiteboardClient() throws RemoteException {
        super();
        image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        graphics = (Graphics2D) image.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    public void start() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            server = (WhiteboardServerInterface) registry.lookup("WhiteboardServer");
            server.addClient(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame = new JFrame("Whiteboard Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        CreateToolbar();
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.drawImage(image, 0, 0, null);

                if (drawing && tempShape != null) {
                    g2d.setColor(Color.BLACK);
                    g2d.draw(tempShape);
                }
            }
        };
        
        panel.setPreferredSize(new java.awt.Dimension(600, 400));
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);

        try {
            renderDrawings(server.getDrawings());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();
                drawing = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                drawing = false;
                int x = e.getX();
                int y = e.getY();
                int width = Math.abs(x - startX);
                int height = Math.abs(y - startY);
                int topLeftX = Math.min(startX, x);
                int topLeftY = Math.min(startY, y);

                try {
                    switch (currentShape) {
                        case LINE:
                            server.broadcastDrawLine(startX, startY, x, y);
                            break;
                        case CIRCLE:
                            server.broadcastDrawCircle(topLeftX, topLeftY, width);
                            break;
                        case RECTANGLE:
                            server.broadcastDrawRectangle(topLeftX, topLeftY, width, height);
                            break;
                        case OVAL:
                            server.broadcastDrawOval(topLeftX, topLeftY, width, height);
                            break;
                        default:
                            break;
                    }
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }

                tempShape = null;
                panel.repaint();
            }
        });

        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                int width = Math.abs(x - startX);
                int height = Math.abs(y - startY);
                int topLeftX = Math.min(startX, x);
                int topLeftY = Math.min(startY, y);

                if (drawing) {
                    switch (currentShape) {
                        case LINE:
                            try {
                                server.broadcastDrawLine(startX, startY, x, y);
                            } catch (RemoteException remoteException) {
                                remoteException.printStackTrace();
                            }
                            startX = x;
                            startY = y;
                            break;
                        case CIRCLE:
                            int diameter = Math.max(width, height);
                            topLeftX = startX < x ? startX : startX - diameter;
                            topLeftY = startY < y ? startY : startY - diameter;
                            tempShape = new Ellipse2D.Double(topLeftX, topLeftY, diameter, diameter);
                            break;
                        case RECTANGLE:
                            topLeftX = startX < x ? startX : x;
                            topLeftY = startY < y ? startY : y;
                            tempShape = new Rectangle2D.Double(topLeftX, topLeftY, width, height);
                            break;
                        case OVAL:
                            topLeftX = startX < x ? startX : x;
                            topLeftY = startY < y ? startY : y;
                            tempShape = new Ellipse2D.Double(topLeftX, topLeftY, width, height);
                            break;
                        default:
                            break;
                    }
                }

                panel.repaint();
            }
        });
    }

    private void CreateToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            try {
                server.clear();
            } catch (RemoteException remoteException) {
                remoteException.printStackTrace();
            }
        });
        toolbar.add(clearButton);
        frame.getContentPane().add(toolbar, java.awt.BorderLayout.NORTH);

        JButton LineButton = new JButton("Line");
        LineButton.addActionListener(e -> currentShape = DrawingShape.LINE);
        toolbar.add(LineButton);
        frame.getContentPane().add(toolbar, java.awt.BorderLayout.NORTH);

        JButton CircleButton = new JButton("Circle");
        CircleButton.addActionListener(e -> currentShape = DrawingShape.CIRCLE);
        toolbar.add(CircleButton);
        frame.getContentPane().add(toolbar, java.awt.BorderLayout.NORTH);

        JButton ovalButton = new JButton("Oval");
        ovalButton.addActionListener(e -> currentShape = DrawingShape.OVAL);
        toolbar.add(ovalButton);
        frame.getContentPane().add(toolbar, java.awt.BorderLayout.NORTH);

        JButton RectangleButton = new JButton("Rectangle");
        RectangleButton.addActionListener(e -> currentShape = DrawingShape.RECTANGLE);
        toolbar.add(RectangleButton);
        frame.getContentPane().add(toolbar, java.awt.BorderLayout.NORTH);
    }

    @Override
    public void draw(Shape shape) throws RemoteException {
        if (shape instanceof Line2D) {
            Line2D line = (Line2D) shape;
            graphics.draw(line);
        } else if (shape instanceof Ellipse2D) {
            Ellipse2D ellipse = (Ellipse2D) shape;
            if (ellipse.getWidth() == ellipse.getHeight()) { // Circle
                graphics.drawOval((int) ellipse.getX(), (int) ellipse.getY(), (int) ellipse.getWidth(), (int) ellipse.getHeight());
            } else { // Oval
                graphics.drawOval((int) ellipse.getX(), (int) ellipse.getY(), (int) ellipse.getWidth(), (int) ellipse.getHeight());
            }
        } else if (shape instanceof Rectangle2D) {
            Rectangle2D rectangle = (Rectangle2D) shape;
            graphics.drawRect((int) rectangle.getX(), (int) rectangle.getY(), (int) rectangle.getWidth(), (int) rectangle.getHeight());
        }
        panel.repaint();
    }

    @Override
    public void renderDrawings(ArrayList<Shape> drawings) throws RemoteException {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        for (Shape l : drawings) {
            graphics.setColor(Color.BLACK);
            graphics.draw(l);
        }
        panel.repaint();
    }

    

    @Override
    public void receiveMessage(String message) throws RemoteException {
        JOptionPane.showMessageDialog(frame, message);
    }

    @Override
    public void clear() throws RemoteException {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        panel.repaint();
    }

    public static void main(String[] args) {
        try {
            WhiteboardClient client = new WhiteboardClient();
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}    
