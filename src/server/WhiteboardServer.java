package server;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.awt.geom.*;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import client.ColoredShape;
import client.WhiteboardClientInterface;


public class WhiteboardServer extends UnicastRemoteObject implements WhiteboardServerInterface {

    private CopyOnWriteArrayList<WhiteboardClientInterface> clients;
    private CopyOnWriteArrayList<ColoredShape> drawings;
    DefaultListModel<String> listModel = new DefaultListModel<>();
    private int clientId = 0;

    public WhiteboardServer() throws RemoteException {
        clients = new CopyOnWriteArrayList<>();
        drawings = new CopyOnWriteArrayList<>();
    }

    public void start() {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("WhiteboardServer", this);
            System.out.println("Server started");
            createServerGUI();
            checkClientsStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createServerGUI() {
        JFrame serverFrame = new JFrame("Whiteboard Server");
        serverFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        serverFrame.setLayout(new BorderLayout());
    
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
    }    

    public boolean displayConnectionRequest() {
        int response = JOptionPane.showConfirmDialog(null, "A client wants to join. Do you accept?", "Connection Request", JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean requestConnection() throws RemoteException {
        return displayConnectionRequest();
    }

    @Override
    public void addClient(WhiteboardClientInterface client) throws RemoteException {
        clients.add(client);
        String clientName = "Client " + clientId;
        clientId++;
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
    public void broadcastDrawLine(int x1, int y1, int x2, int y2, int color) throws RemoteException {
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
        ColoredShape coloredLine = new ColoredShape(line, new Color(color));
        drawings.add(coloredLine);
        for (WhiteboardClientInterface client : clients) {
            client.draw(coloredLine);
            client.renderDrawings(drawings);
        }
    }

    @Override
    public void broadcastDrawCircle(int x, int y, int diameter, int color) throws RemoteException {
        Ellipse2D circle = new Ellipse2D.Double(x, y, diameter, diameter);
        ColoredShape coloredCircle = new ColoredShape(circle, new Color(color));
        drawings.add(coloredCircle);
        for (WhiteboardClientInterface client : clients) {
            client.draw(coloredCircle);
            client.renderDrawings(drawings);
        }
    }
    
    @Override
    public void broadcastDrawOval(int x, int y, int width, int height, int color) throws RemoteException {
        Ellipse2D oval = new Ellipse2D.Double(x, y, width, height);
        ColoredShape coloredOval = new ColoredShape(oval, new Color(color));
        drawings.add(coloredOval);
        for (WhiteboardClientInterface client : clients) {
            client.draw(coloredOval);
            client.renderDrawings(drawings);
        }
    }
    
    @Override
    public void broadcastDrawRectangle(int x, int y, int width, int height, int color) throws RemoteException {
        Rectangle2D rectangle = new Rectangle2D.Double(x, y, width, height);
        ColoredShape coloredRectangle = new ColoredShape(rectangle, new Color(color));
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
    
    

    public static void main(String[] args) {
        try {
            WhiteboardServer server = new WhiteboardServer();
            System.out.println("Whiteboard server is running...");
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
