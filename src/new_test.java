import java.awt.*;

public class new_test {
    public static void main(String[] args) {
        Text2D text = new Text2D("Hello World!", 0, 0, Color.BLACK, new Font("Arial", Font.PLAIN, 12));
        System.out.println(text.getText());
        System.out.println(text.getColor());
        System.out.println(text.getFont());
        text.setText("Goodbye World!");
        System.out.println(text.getText());
    }
}
