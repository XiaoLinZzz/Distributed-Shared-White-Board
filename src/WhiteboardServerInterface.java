import java.rmi.Remote;
import java.rmi.RemoteException;
import java.awt.Shape;
import java.util.concurrent.CopyOnWriteArrayList;

public interface WhiteboardServerInterface extends Remote {
    void addClient(WhiteboardClientInterface client) throws RemoteException;

    void removeClient(WhiteboardClientInterface client) throws RemoteException;

    void broadcastMessage(String message) throws RemoteException;

    void broadcastDrawLine(int x1, int y1, int x2, int y2, int color) throws RemoteException;

    void clear() throws RemoteException;

    void broadcastDrawCircle(int x, int y, int diameter, int color) throws RemoteException;

    void broadcastDrawOval(int x, int y, int width, int height, int color) throws RemoteException;

    void broadcastDrawRectangle(int x, int y, int width, int height, int color) throws RemoteException;

    CopyOnWriteArrayList<Shape> getDrawings() throws RemoteException;
    
    boolean requestConnection() throws RemoteException;
    
}
