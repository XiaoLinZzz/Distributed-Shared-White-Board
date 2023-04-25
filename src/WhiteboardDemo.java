import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class WhiteboardDemo extends JPanel {

    private int startX, startY;
    private boolean textInputMode = true;
    private Color currentColor = Color.BLACK;
    private Graphics2D graphics;

    public WhiteboardDemo() {
        setFocusable(true);
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (textInputMode) {
                    startX = e.getX();
                    startY = e.getY();
                    requestFocusInWindow();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (textInputMode) {
                    String text = String.valueOf(e.getKeyChar());
                    Font currentFont = new Font("Arial", Font.PLAIN, 20);
                    graphics = (Graphics2D) getGraphics();
                    graphics.setColor(currentColor);
                    graphics.setFont(currentFont);
                    graphics.drawString(text, startX, startY);
                    startX += graphics.getFontMetrics(currentFont).stringWidth(text);
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Whiteboard Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new WhiteboardDemo());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
