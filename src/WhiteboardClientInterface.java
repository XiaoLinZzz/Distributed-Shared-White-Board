
import java.awt.Shape;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.CopyOnWriteArrayList;

public interface WhiteboardClientInterface extends Remote {
    void draw(Shape shape) throws RemoteException;

    void renderDrawings(CopyOnWriteArrayList<Shape> drawings) throws RemoteException;

    void receiveMessage(String message) throws RemoteException;

    void clear() throws RemoteException;
}
