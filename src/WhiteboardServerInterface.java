
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.CopyOnWriteArrayList;

public interface WhiteboardServerInterface extends Remote {
    void addClient(WhiteboardClientInterface client, String userName) throws RemoteException;

    void removeClient(WhiteboardClientInterface client) throws RemoteException;

    void broadcastMessage(String message) throws RemoteException;

    void broadcastDrawLine(int x1, int y1, int x2, int y2, int color) throws RemoteException;

    void clear() throws RemoteException;

    void broadcastDrawCircle(int x, int y, int diameter, int color) throws RemoteException;

    void broadcastDrawOval(int x, int y, int width, int height, int color) throws RemoteException;

    void broadcastDrawText(String text, int x, int y, int color) throws RemoteException;

    void broadcastDrawRectangle(int x, int y, int width, int height, int color) throws RemoteException;

    CopyOnWriteArrayList<ColoredShape> getDrawings() throws RemoteException;
    
    boolean requestConnection(String userName) throws RemoteException;

    CopyOnWriteArrayList<String> getChat() throws RemoteException;

    void addChatMessage(String userName, String message) throws RemoteException;
}
