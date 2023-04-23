


import java.awt.Color;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.CopyOnWriteArrayList;

public interface WhiteboardClientInterface extends Remote {
    void draw(ColoredShape shape) throws RemoteException;

    void renderDrawings(CopyOnWriteArrayList<ColoredShape> drawings) throws RemoteException;

    void receiveMessage(String message) throws RemoteException;

    void clear() throws RemoteException;

    void closeApplication() throws RemoteException;

    void setCurrentColor(Color color) throws RemoteException;

    boolean ping() throws RemoteException;
}
