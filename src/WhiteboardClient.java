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

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class WhiteboardClient extends UnicastRemoteObject implements WhiteboardClientInterface {
    private JFrame frame;
    private JPanel panel;
    private JLabel statusLabel;
    private BufferedImage image;
    private Graphics2D graphics;
    private WhiteboardServerInterface server;
    private int startX, startY;
    private Shape tempShape = null;
    private boolean drawing = false;
    private Color currentColor = Color.BLACK;
    private DrawingShape currentShape = DrawingShape.LINE;
    private JTextArea chatArea;
    private JTextField chatField;
    private JPanel chatPanel;
    private String userName;
    private boolean serverRunning = true;

    public enum DrawingShape {
        LINE,
        CIRCLE,
        OVAL,
        RECTANGLE;
    }

    public WhiteboardClient(String serverIPAddress, int serverPort, String userName) throws RemoteException {
        super();
        this.userName = userName;
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
            boolean connectionAllowed = server.requestConnection(userName);
            if (!connectionAllowed) {
                try {
                    connectionAllowed = server.requestConnection(userName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            server.addClient(this, userName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        frame = new JFrame("Whiteboard Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JToolBar toolbar = CreateToolbar();
        // frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        JToolBar colorChooser = CreateColorChooser();
        // frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.drawImage(image, 0, 0, null);

                if (drawing && tempShape != null) {
                    g2d.setColor(currentColor);
                    g2d.draw(tempShape);
                }
            }
        };
        panel.setPreferredSize(new java.awt.Dimension(600, 400));

        JPanel drawingPanel = new JPanel();
        drawingPanel.setLayout(new BoxLayout(drawingPanel, BoxLayout.Y_AXIS));
        drawingPanel.add(toolbar);
        drawingPanel.add(colorChooser);
        drawingPanel.add(panel);

        createChat();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drawingPanel, chatPanel);
        splitPane.setResizeWeight(0.75);
        frame.setContentPane(splitPane);
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

                if (!serverRunning) {
                    return;
                }

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
                            server.broadcastDrawLine(startX, startY, x, y, currentColor.getRGB());
                            break;
                        case CIRCLE:
                            int diameter = Math.max(width, height);
                            topLeftX = startX < x ? startX : startX - diameter;
                            topLeftY = startY < y ? startY : startY - diameter;
                            server.broadcastDrawCircle(topLeftX, topLeftY, diameter, currentColor.getRGB());
                            break;
                        case RECTANGLE:
                            server.broadcastDrawRectangle(topLeftX, topLeftY, width, height, currentColor.getRGB());
                            break;
                        case OVAL:
                            server.broadcastDrawOval(topLeftX, topLeftY, width, height, currentColor.getRGB());
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


        // Preview the shape while drawing
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
                                server.broadcastDrawLine(startX, startY, x, y, currentColor.getRGB());
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


    // CHAT
    private void createChat() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatAreaScrollPane = new JScrollPane(chatArea);

        chatField = new JTextField();
        chatField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (!serverRunning) {
                    return;
                }

                String message = chatField.getText().trim();
                if (!message.isEmpty()) {
                    try {
                        renderChatMessages(message);
                        chatField.setText("");
                    } catch (RemoteException remoteException) {
                        remoteException.printStackTrace();
                    }
                }
            }
        });

        chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        chatPanel.add(chatAreaScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatField, BorderLayout.SOUTH);
    }

    @Override
    public void setCurrentColor(Color color) throws RemoteException {
        currentColor = color;
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

    // TOOL BAR
    private JToolBar CreateToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setLayout(new BorderLayout());

        statusLabel = new JLabel("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        toolbar.add(statusLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);    

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            try {
                server.clear();
            } catch (RemoteException remoteException) {
                remoteException.printStackTrace();
            }
        });
        buttonPanel.add(clearButton);

        // toolbar.addSeparator();

        JButton LineButton = new JButton("Line");
        LineButton.addActionListener(e -> {
            currentShape = DrawingShape.LINE;
            statusLabel.setText("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        });
        buttonPanel.add(LineButton);

        // toolbar.addSeparator();

        JButton CircleButton = new JButton("Circle");
        CircleButton.addActionListener(e -> {
            currentShape = DrawingShape.CIRCLE;
            statusLabel.setText("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        });
        buttonPanel.add(CircleButton);

        // toolbar.addSeparator();

        JButton ovalButton = new JButton("Oval");
        ovalButton.addActionListener(e -> {
            currentShape = DrawingShape.OVAL;
            statusLabel.setText("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        });        
        buttonPanel.add(ovalButton);
        // toolbar.addSeparator();

        JButton RectangleButton = new JButton("Rectangle");
        RectangleButton.addActionListener(e -> {
            currentShape = DrawingShape.RECTANGLE;
            statusLabel.setText("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        });
        buttonPanel.add(RectangleButton);

        toolbar.add(buttonPanel, BorderLayout.CENTER);

        frame.getContentPane().add(toolbar, BorderLayout.NORTH);

        return toolbar;
    }

    private void setCurrentColorStatus(Color color) {
        currentColor = color;
        statusLabel.setText("Current shape: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
    }

    // COLOR CHOOSER
    private JToolBar CreateColorChooser() {
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

        JToolBar colorChooserToolbar = new JToolBar();
        colorChooserToolbar.setFloatable(false);
        colorChooserToolbar.setRollover(true);
        colorChooserToolbar.add(colorPanel);
        frame.getContentPane().add(colorChooserToolbar, BorderLayout.EAST);

        return colorChooserToolbar;
    }

    @Override
    public void draw(ColoredShape coloredShape) throws RemoteException {
        Shape shape = coloredShape.getShape();
        Color color = coloredShape.getColor();
    
        if (shape instanceof Line2D) {
            Line2D line = (Line2D) shape;
            graphics.setColor(color);
            graphics.draw(line);
        } else if (shape instanceof Ellipse2D) {
            Ellipse2D ellipse = (Ellipse2D) shape;
            if (ellipse.getWidth() == ellipse.getHeight()) { // Circle
                graphics.setColor(color);
                graphics.drawOval((int) ellipse.getX(), (int) ellipse.getY(), (int) ellipse.getWidth(), (int) ellipse.getHeight());
            } else { // Oval
                graphics.setColor(color);
                graphics.drawOval((int) ellipse.getX(), (int) ellipse.getY(), (int) ellipse.getWidth(), (int) ellipse.getHeight());
            }
        } else if (shape instanceof Rectangle2D) {
            Rectangle2D rectangle = (Rectangle2D) shape;
            graphics.setColor(color);
            graphics.drawRect((int) rectangle.getX(), (int) rectangle.getY(), (int) rectangle.getWidth(), (int) rectangle.getHeight());
        }
        panel.repaint();
    }
    

    @Override
    public void renderDrawings(CopyOnWriteArrayList<ColoredShape> drawings) throws RemoteException {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        
        for (ColoredShape coloredShape : drawings) {
            draw(coloredShape);
        }
        
        panel.repaint();
    }
    
    

    @Override
    public void receiveMessage(String message) throws RemoteException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (message.equals("You have been removed from the server")) {
                    int result = JOptionPane.showOptionDialog(frame, message, "Disconnected", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, null, null);
                    if (result == JOptionPane.OK_OPTION) {
                        try {
                            closeApplication();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, message);
                }
            }
        });
    }
    

    @Override
    public void clear() throws RemoteException {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        panel.repaint();
    }

    @Override
    public void closeApplication() throws RemoteException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                WhiteboardClient.this.frame.dispose();
            }
        });

        System.exit(0);
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    @Override
    public void renderChatMessages(CopyOnWriteArrayList<String> messages) throws RemoteException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chatArea.setText("");
                for (String message : messages) {
                    chatArea.append(message + "\n");
                }
            }
        });
    }


    // @Override
    // public void receiveChatMessage(String message) throws RemoteException {
    //     SwingUtilities.invokeLater(new Runnable() {
    //         @Override
    //         public void run() {
    //             chatArea.append(message + "\n");
    //         }
    //     });
    // }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java JoinWhiteBoard <serverIPAddress> <serverPort> <username>");
            return;
        }
    
        String serverIPAddress = args[0];
        int serverPort;
        try {
            serverPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid server port");
            return;
        }
        String username = args[2];
    
        try {
            WhiteboardClient client = new WhiteboardClient(serverIPAddress, serverPort, username);
            client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}