package acluadev;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

public class Console {
    public final JTextPane textPane;

    private Console(JTextPane textPane) {
        this.textPane = textPane;
    }

    private static InputStream getNonNullResourceStream(String path) {
        return Objects.requireNonNull(Console.class.getClassLoader().getResourceAsStream(path));
    }

    public static Console createConsole() {
        var f = new JFrame("AC Test Harness");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try {
            var img = ImageIO.read(getNonNullResourceStream("icon.png"));
            f.setIconImage(img);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        f.setVisible(true);
        f.setBackground(Color.BLACK);

        var textPane = new JTextPane();
        textPane.setBackground(Color.BLACK);
        textPane.setCaretColor(Color.WHITE);
        textPane.setForeground(Color.WHITE);
        Font font;
        try {
            font = Font.createFonts(getNonNullResourceStream("DejavuSansMono-5m7L.ttf"))[0];
        }
        catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
        textPane.setFont(font.deriveFont(24f));
        textPane.setEditable(false);
        var caret = (DefaultCaret)textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        var scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(new LineBorder(Color.BLACK));
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        f.add(scrollPane, BorderLayout.CENTER);

        var d = new Dimension(1000,520);
        f.setResizable(false);
        f.setSize(d);
        f.setMaximumSize(d);
        f.setMinimumSize(d);

        return new Console(textPane);
    }

    public void println(String s) {
        var d = textPane.getStyledDocument();
        try {
            d.insertString(d.getLength(), s+"\n", null);
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }

    }
}
