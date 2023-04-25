import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public class Text2D extends Rectangle2D.Double {
    private String text;
    private Color color;
    private Font font;

    public Text2D(String text, int x, int y, Color color, Font Font) {
        super(x, y, 0, 0);
        this.text = text;
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Color getColor() {
        return color;
    }

    public Font getFont() {
        return font;
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.setFont(font);
        g2d.drawString(text, (float) x, (float) y);
    }
}
