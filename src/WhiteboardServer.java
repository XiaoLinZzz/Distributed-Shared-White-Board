import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.awt.Shape;
import java.awt.geom.*;
import java.util.concurrent.CopyOnWriteArrayList;
// import java.awt.*;


public class WhiteboardServer extends UnicastRemoteObject implements WhiteboardServerInterface {

    private CopyOnWriteArrayList<WhiteboardClientInterface> clients;
    private CopyOnWriteArrayList<Shape> drawings;

    public WhiteboardServer() throws RemoteException {
        clients = new CopyOnWriteArrayList<>();
        drawings = new CopyOnWriteArrayList<>();
    }

    public void start() {
        try {
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.bind("WhiteboardServer", this);
            System.out.println("Server started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addClient(WhiteboardClientInterface client) throws RemoteException {
        clients.add(client);
        System.out.println("Client added");
    }

    @Override
    public void removeClient(WhiteboardClientInterface client) throws RemoteException {
        clients.remove(client);
        System.out.println("Client removed");
    }

    @Override
    public void broadcastDrawLine(int x1, int y1, int x2, int y2) throws RemoteException {
        Line2D line = new Line2D.Double(x1, y1, x2, y2);
        drawings.add(line);
        for (WhiteboardClientInterface client : clients) {
            client.draw(line);
            client.renderDrawings(drawings);
        }
    }

    @Override
    public void broadcastDrawCircle(int x, int y, int diameter) throws RemoteException {
        Ellipse2D circle = new Ellipse2D.Double(x, y, diameter, diameter);
        drawings.add(circle);
        for (WhiteboardClientInterface client : clients) {
            client.draw(circle);
            client.renderDrawings(drawings);
        }
    }
    
    @Override
    public void broadcastDrawOval(int x, int y, int width, int height) throws RemoteException {
        Ellipse2D oval = new Ellipse2D.Double(x, y, width, height);
        drawings.add(oval);
        for (WhiteboardClientInterface client : clients) {
            client.draw(oval);
            client.renderDrawings(drawings);
        }
    }
    
    @Override
    public void broadcastDrawRectangle(int x, int y, int width, int height) throws RemoteException {
        Rectangle2D rectangle = new Rectangle2D.Double(x, y, width, height);
        drawings.add(rectangle);
        for (WhiteboardClientInterface client : clients) {
            client.draw(rectangle);
            client.renderDrawings(drawings);
        }
    }
    
    @Override
    public CopyOnWriteArrayList<Shape> getDrawings() throws RemoteException {
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

    public static void main(String[] args) {
        try {
            WhiteboardServer server = new WhiteboardServer();
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
