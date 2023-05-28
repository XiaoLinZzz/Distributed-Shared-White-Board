import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.awt.geom.*;
import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class WhiteboardServer extends UnicastRemoteObject implements WhiteboardServerInterface {

    private CopyOnWriteArrayList<WhiteboardClientInterface> clients;
    private CopyOnWriteArrayList<ColoredShape> drawings;
    private CopyOnWriteArrayList<String> chat;
    DefaultListModel<String> listModel = new DefaultListModel<>();
    private File currentFile;
    private String hostUserName;

    public WhiteboardServer(String serverIPAddress, int serverPort, String userName) throws RemoteException {
        clients = new CopyOnWriteArrayList<>();
        drawings = new CopyOnWriteArrayList<>();
        chat = new CopyOnWriteArrayList<>();
    }

    public void start(int serverPort, String userName) {
        try {
            this.hostUserName = userName;
            Registry registry = LocateRegistry.createRegistry(serverPort);
            registry.bind("WhiteboardServer", this);
            System.out.println("Server started");
            createServerGUI(userName);

            WhiteboardClient client = new WhiteboardClient("localhost", serverPort, userName);
            client.start("localhost", serverPort);

            checkClientsStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createServerGUI(String userName) {
        JFrame serverFrame = new JFrame("Whiteboard Server" + " - " + userName);
        serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverFrame.setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();

        //Build the file menu.
        JMenu menu = new JMenu("File");
        menuBar.add(menu);
    
        JMenuItem menuItem = new JMenuItem("New");
        menuItem.addActionListener(e -> { 
            // Clear the current file
            currentFile = null;
            serverFrame.setTitle(userName);
            drawings.clear();
            // System.out.println("New file created");
            JOptionPane.showMessageDialog(serverFrame, "New file created");

            for (WhiteboardClientInterface client : clients) {
                try {
                    client.renderDrawings(drawings);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        menu.add(menuItem);
    
        menuItem = new JMenuItem("Open");
        menuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(serverFrame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                serverFrame.setTitle(userName + " - " + currentFile.getName());
            
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(currentFile))) {
                    drawings.clear();  // clear the previous drawings
                    while (true) {
                        try {
                            ColoredShape drawing = (ColoredShape) ois.readObject();
                            drawings.add(drawing);
                        } catch (EOFException ex) {
                            // We've reached the end of the file, break the loop
                            break;
                        }
                    }
                    JOptionPane.showMessageDialog(serverFrame, "File opened successfully");
                    
                    // Now inform all clients about the new drawings
                    for (WhiteboardClientInterface client : clients) {
                        try {
                            client.renderDrawings(drawings);
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        });
        menu.add(menuItem);
        
        
        
    
        menuItem = new JMenuItem("Save");
        menuItem.addActionListener(e -> {
            if (currentFile == null) {
                // If no file is currently opened, show "Save As" dialog
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showSaveDialog(serverFrame);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    currentFile = fileChooser.getSelectedFile();
                    serverFrame.setTitle(userName + " - " + currentFile.getName());
                    // System.out.println("Saving to " + currentFile.getName());
                    JOptionPane.showMessageDialog(serverFrame, "Saving to " + currentFile.getName());
                } else {
                    // User did not select a file, abort save operation
                    // System.out.println("Save aborted");
                    JOptionPane.showMessageDialog(serverFrame, "Save aborted");
                    return;
                }
            }
            // Save the state to the current file
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(currentFile))) {
                for (ColoredShape drawing : drawings) {
                    oos.writeObject(drawing);
                }
                // System.out.println(currentFile.getName() + " saved successfully");
                JOptionPane.showMessageDialog(serverFrame, "File saved successfully");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        menu.add(menuItem);
        
        
    
        menuItem = new JMenuItem("Save As");
        menuItem.addActionListener(e -> {
            // Show "Save As" dialog
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showSaveDialog(serverFrame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                currentFile = fileChooser.getSelectedFile();
                serverFrame.setTitle(userName + " - " + currentFile.getName());
        
                // Save the state to the selected file
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(currentFile))) {
                    for (ColoredShape drawing : drawings) {
                        oos.writeObject(drawing);
                    }
                    // System.out.println(currentFile.getName() + " saved successfully");
                    JOptionPane.showMessageDialog(serverFrame, "File saved successfully");
                    
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        menu.add(menuItem);
        
    
        menuItem = new JMenuItem("Close");
        menuItem.addActionListener(e -> {
            // Close the current file
            currentFile = null;
            serverFrame.setTitle(userName);
            drawings.clear();
            // System.out.println("File closed");
            JOptionPane.showMessageDialog(serverFrame, "File closed");

            for (WhiteboardClientInterface client : clients) {
                try {
                    client.renderDrawings(drawings);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        menu.add(menuItem);
    
        serverFrame.setJMenuBar(menuBar);
    
    
        JList<String> clientList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(clientList);
        serverFrame.add(scrollPane, BorderLayout.CENTER);
    
        JLabel infoLabel = new JLabel("Connected clients:");
        serverFrame.add(infoLabel, BorderLayout.NORTH);

        JButton removeClientButton = new JButton("Remove Client");
        serverFrame.add(removeClientButton, BorderLayout.SOUTH);
    
        serverFrame.setSize(300, 400);
        serverFrame.setVisible(true);

        removeClientButton.addActionListener(e -> {
            int selectedIndex = clientList.getSelectedIndex();
            if (selectedIndex != -1) {
                try {
                    clients.get(selectedIndex).clear();
                    clients.get(selectedIndex).receiveMessage("You have been removed from the server");
                    clients.get(selectedIndex).renderDrawings(drawings);
                    removeClient(clients.get(selectedIndex));
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
    
        clientList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedIndex = clientList.getSelectedIndex();
                    if (selectedIndex != -1) {
                        removeClientButton.setEnabled(true);
                    } else {
                        removeClientButton.setEnabled(false);
                    }
                }
            }
        });

        serverFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                for (WhiteboardClientInterface client : clients) {
                    try {
                        // client.closeApplication();
                        client.receiveMessage("Server has been closed");

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }    

    public boolean displayConnectionRequest(String userName) {
        int response = JOptionPane.showConfirmDialog(null, userName + " wants to join. Do you accept?", "Connection Request", JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean requestConnection(String userName) throws RemoteException {
        if(userName.equals(hostUserName)) {
            return true;
        } else {
            return displayConnectionRequest(userName);
        }
    }
    

    @Override
    public void addClient(WhiteboardClientInterface client, String userName) throws RemoteException {
        clients.add(client);
        String clientName = userName;
        System.out.println(clientName + " added");
        listModel.addElement(clientName);
    }

    @Override
    public void removeClient(WhiteboardClientInterface client) throws RemoteException {
        int index = clients.indexOf(client);
        clients.remove(client);
        System.out.println("Client removed");
        if (index != -1) {
            listModel.remove(index);
        }
    }

    @Override
    public void broadcastDrawText(String text, int x, int y, int color) throws RemoteException {
        Font font = new Font("Arial", Font.PLAIN, 20); 
        Text2D text2D = new Text2D(text, x, y, new Color(color), font);
        ColoredShape coloredText = new ColoredShape(text2D, new Color(color), text);
        drawings.add(coloredText);
        for (WhiteboardClientInterface client : clients) {
            client.draw(coloredText);
            client.renderDrawings(drawings);
        }
    }
    

    @Override
    public void broadcastDrawLine(int x1, int y1, int x2, int y2, int color) throws RemoteException {
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
        ColoredShape coloredLine = new ColoredShape(line, new Color(color), null);
        drawings.add(coloredLine);
        for (WhiteboardClientInterface client : clients) {
            client.draw(coloredLine);
            client.renderDrawings(drawings);
        }
    }

    @Override
    public void broadcastDrawCircle(int x, int y, int diameter, int color) throws RemoteException {
        Ellipse2D circle = new Ellipse2D.Double(x, y, diameter, diameter);
        ColoredShape coloredCircle = new ColoredShape(circle, new Color(color), null);
        drawings.add(coloredCircle);
        for (WhiteboardClientInterface client : clients) {
            client.draw(coloredCircle);
            client.renderDrawings(drawings);
        }
    }
    
    @Override
    public void broadcastDrawOval(int x, int y, int width, int height, int color) throws RemoteException {
        Ellipse2D oval = new Ellipse2D.Double(x, y, width, height);
        ColoredShape coloredOval = new ColoredShape(oval, new Color(color), null);
        drawings.add(coloredOval);
        for (WhiteboardClientInterface client : clients) {
            client.draw(coloredOval);
            client.renderDrawings(drawings);
        }
    }
    
    @Override
    public void broadcastDrawRectangle(int x, int y, int width, int height, int color) throws RemoteException {
        Rectangle2D rectangle = new Rectangle2D.Double(x, y, width, height);
        ColoredShape coloredRectangle = new ColoredShape(rectangle, new Color(color), null);
        drawings.add(coloredRectangle);
        for (WhiteboardClientInterface client : clients) {
            client.draw(coloredRectangle);
            client.renderDrawings(drawings);
        }
    }
    
    @Override
    public CopyOnWriteArrayList<ColoredShape> getDrawings() throws RemoteException {
        return drawings;
    }

    @Override
    public CopyOnWriteArrayList<String> getChat() throws RemoteException {
        return chat;
    }
    

    @Override
    public void clear() throws RemoteException {
        drawings.clear();
        for (WhiteboardClientInterface client : clients) {
            client.clear();
        }
    }

    @Override
    public void broadcastMessage(String message) throws RemoteException {
        for (WhiteboardClientInterface client : clients) {
            client.receiveMessage(message);
        }
    }

    private void checkClientsStatus() {
        Timer timer = new Timer(5, e -> {
            for (int i = 0; i < clients.size(); i++) {
                WhiteboardClientInterface client = clients.get(i);
                try {
                    if (!client.ping()) {
                        removeClient(client);
                    }
                } catch (RemoteException ex) {
                    try {
                        removeClient(client);
                    } catch (RemoteException rex) {
                        rex.printStackTrace();
                    }
                }
            }
        });
        timer.setInitialDelay(0);
        timer.start();
    }

    @Override
    public void addChatMessage(String userName, String message) throws RemoteException {
        // Generate a timestamp for the message
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timeString = now.format(formatter);
    
        String fullMessage = "[" + timeString + "] " + userName + ": " + message;
        System.out.println("Adding chat message: " + fullMessage);
        chat.add(fullMessage);
        for (WhiteboardClientInterface client : clients) {
            try {
                client.renderChatMessages(chat);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void printAllDrawingTexts() {
        for (ColoredShape drawing : drawings) {
            if (drawing.getText() != null) {
                System.out.println("Drawing Text: " + drawing.getText());
            }
        }
    }
    

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java CreateWhiteBoard <serverIPAddress> <port> <username>");
            System.exit(1);
        }

        String serverIPAddress = args[0];
        int port = Integer.parseInt(args[1]);
        String userName = args[2];

        try {
            WhiteboardServer server = new WhiteboardServer(serverIPAddress, port, userName);
            System.out.println("Whiteboard server is running...");
            server.start(port, userName);

            server.printAllDrawingTexts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
