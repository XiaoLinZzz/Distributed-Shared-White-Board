package client;
import java.awt.*;
import java.io.Serializable;

public class ColoredShape implements Serializable {
    private static final long serialVersionUID = 1L; // Add this to ensure consistent serialization across different systems

    private Shape shape;
    private Color color;

    public ColoredShape(Shape shape, Color color) {
        this.shape = shape;
        this.color = color;
    }

    public Shape getShape() {
        return shape;
    }

    public Color getColor() {
        return color;
    }
}
