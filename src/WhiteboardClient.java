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
import java.net.ConnectException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.CopyOnWriteArrayList;


public class WhiteboardClient extends UnicastRemoteObject implements WhiteboardClientInterface {
    private JFrame frame;
    private JPanel panel;
    private JLabel statusLabel;
    private JLabel textModeLabel;
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
    private boolean textInputMode = false;
    private boolean serverRunning = true;
    

    public enum DrawingShape {
        TEXT,
        LINE,
        CIRCLE,
        OVAL,
        RECTANGLE;
    }

    public WhiteboardClient(String serverIPAddress, int serverPort, String userName) throws RemoteException {
        super();
        this.userName = userName;

        image = new BufferedImage(690, 400, BufferedImage.TYPE_INT_RGB);
        graphics = (Graphics2D) image.getGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    public void start(String serverIPAddress, int serverPort) {
        try {
            Registry registry = LocateRegistry.getRegistry(serverIPAddress, serverPort);
            server = (WhiteboardServerInterface) registry.lookup("WhiteboardServer");

            boolean connectionAllowed = server.requestConnection(userName);
            if (connectionAllowed) {
                server.addClient(this, userName);
            } else {
                JOptionPane.showMessageDialog(null, "Connection not allowed");
                System.exit(0);
            }

        } catch (RemoteException e) {
            if (e.getCause() instanceof ConnectException) {
                JOptionPane.showMessageDialog(null, "Connection refused. Please check the server IP, port, and ensure the server is running.");
            } else {
                JOptionPane.showMessageDialog(null, "Connection failed. Please check the server IP and port.");
            }
            e.printStackTrace();
            System.exit(0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Connection failed. Please check the server IP and port.");
            e.printStackTrace();
            System.exit(0);
        } 

        frame = new JFrame("Whiteboard Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JToolBar toolbar = CreateToolbar();
        JToolBar colorChooser = CreateColorChooser();

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
        panel.setPreferredSize(new java.awt.Dimension(850, 400));

        JPanel drawingPanel = new JPanel();
        drawingPanel.setLayout(new BoxLayout(drawingPanel, BoxLayout.Y_AXIS));
        drawingPanel.add(toolbar);
        drawingPanel.add(colorChooser);
        drawingPanel.add(panel);

        createChat();
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drawingPanel, chatPanel);
        // splitPane.setResizeWeight(0.75);
        splitPane.setDividerLocation(700);
        frame.setContentPane(splitPane);
        frame.pack();
        frame.setVisible(true);

        try {
            renderDrawings(server.getDrawings());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        try {
            renderChatMessages(server.getChat());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!serverRunning || currentShape == null) {
                    return;
                }
            
                startX = e.getX();
                startY = e.getY();
                drawing = true;
            }
            

            @Override
            public void mouseReleased(MouseEvent e) {

                if (!serverRunning || currentShape == null || textInputMode) {
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
           
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (!serverRunning || !textInputMode) {
                    return;
                } else {
                    String text = String.valueOf(e.getKeyChar());
                    // JOptionPane.showMessageDialog(null, "Text input mode. Press enter to send message.");
                    Font currentFont = new Font("Arial", Font.PLAIN, 20);
                    try {
                        server.broadcastDrawText(text, startX, startY, currentColor.getRGB());
                        startX += graphics.getFontMetrics(currentFont).stringWidth(text);
                    } catch (RemoteException remoteException) {
                        remoteException.printStackTrace();
                    }
                }
            }
        });


        // Preview the shape while drawing
        panel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentShape == DrawingShape.TEXT) {
                    return;
                }

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

        JLabel chatTitle = new JLabel("Chat Room");
        chatTitle.setHorizontalAlignment(SwingConstants.CENTER);
        // chatTitle.setFont(new Font("Tahoma", Font.BOLD, 14));

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    

        chatField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        chatPanel.add(chatTitle, BorderLayout.NORTH);
        chatPanel.add(chatAreaScrollPane, BorderLayout.CENTER);
    
        // Create a panel for the chat field and send button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(chatField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
    
        chatPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    @Override
    public void setCurrentColor(Color color) throws RemoteException {
        currentColor = color;
    }

    // TOOL BAR
    private JToolBar CreateToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setLayout(new BorderLayout());

        statusLabel = new JLabel("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        toolbar.add(statusLabel, BorderLayout.WEST);

        textModeLabel = new JLabel("Text Mode: OFF");
        toolbar.add(textModeLabel, BorderLayout.EAST);

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

        JButton lineButton = new JButton("Line");
        lineButton.addActionListener(e -> {
            currentShape = DrawingShape.LINE;
            statusLabel.setText("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        });
        buttonPanel.add(lineButton);

        JButton CircleButton = new JButton("Circle");
        CircleButton.addActionListener(e -> {
            currentShape = DrawingShape.CIRCLE;
            statusLabel.setText("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        });
        buttonPanel.add(CircleButton);

        JButton ovalButton = new JButton("Oval");
        ovalButton.addActionListener(e -> {
            currentShape = DrawingShape.OVAL;
            statusLabel.setText("Current Status: " + currentShape.toString() + " | Current Color: " + colorToString(currentColor));
        });        
        buttonPanel.add(ovalButton);

        JButton RectangleButton = new JButton("Rectangle");
        RectangleButton.addActionListener(e -> {
            currentShape = DrawingShape.RECTANGLE;
            statusLabel.setText("Current Status: Rect | Current Color: " + colorToString(currentColor));
        });
        buttonPanel.add(RectangleButton);

        JToggleButton textToggleButton = new JToggleButton("Text Mode");
        textToggleButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                // Type mode is enabled
                textInputMode = true;
                textModeLabel.setText("Text Mode: ON"); 
            } else {
                // Type mode is disabled
                textInputMode = false;
                textModeLabel.setText("Text Mode: OFF");
            }
        });
        buttonPanel.add(textToggleButton);

        toolbar.add(buttonPanel, BorderLayout.CENTER);
        frame.getContentPane().add(toolbar, BorderLayout.NORTH);
        return toolbar;
    }

    private void setCurrentColorStatus(Color color) {
        currentColor = color;
        String shapeString = (currentShape != null) ? currentShape.toString() : "No Shape Selected";
        statusLabel.setText("Current shape: " + shapeString + " | Current Color: " + colorToString(currentColor));
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
        } else if (shape instanceof Text2D) {
            Text2D text = (Text2D) shape;
            graphics = image.createGraphics();
            graphics.setColor(color);
            graphics.setFont(text.getFont());
            graphics.drawString(text.getText(), (int) text.getX(), (int) text.getY());
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
                } else if (message.equals("Server has been closed")){
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

    private void sendMessage() {
        if (!serverRunning) {
            return;
        }

        String message = chatField.getText().trim();
        if (!message.isEmpty()) {
            try {
                server.addChatMessage(userName, message);
                chatField.setText("");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
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
            client.start(serverIPAddress, serverPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}