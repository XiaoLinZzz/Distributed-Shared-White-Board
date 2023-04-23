
import java.awt.*;
import java.io.Serializable;

public class ColoredShape implements Serializable {
    private static final long serialVersionUID = 1L; // Add this to ensure consistent serialization across different systems

    private Shape shape;
    private Color color;
    private String text;

    public ColoredShape(Shape shape, Color color) {
        this.shape = shape;
        this.color = color;
        this.text = null;
    }

    public Shape getShape() {
        return shape;
    }

    public Color getColor() {
        return color;
    }
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
