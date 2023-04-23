import java.awt.geom.Rectangle2D;

public class Text2D extends Rectangle2D.Double {

    private String text;

    public Text2D(String text, int x, int y) {
        super(x, y, 0, 0);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
