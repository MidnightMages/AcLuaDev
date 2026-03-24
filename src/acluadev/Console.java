package acluadev;

import acluadev.misc.TextBufferUD;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Consumer;

import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;

public class Console implements KeyListener {
    public final JTextPane textPane;
    public Consumer<KeyEvent> onKeyTyped = null;
    public Consumer<KeyEvent> onKeyPressed = null;
    public Consumer<KeyEvent> onKeyReleased = null;

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        f.setBackground(Color.BLACK);

        var textPane = new JTextPane();
        textPane.setBackground(Color.BLACK);
        textPane.setForeground(Color.WHITE);
        Font font;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, getNonNullResourceStream("acfont-firacode-regular.ttf"));
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }
        textPane.setFont(font.deriveFont(14f));
        textPane.setEditable(false);
        var caret = (DefaultCaret) textPane.getCaret();
        caret.setVisible(false);

        var scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(new LineBorder(Color.BLACK));
        scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setAutoscrolls(true);
        f.add(scrollPane, BorderLayout.CENTER);

        // var d = new Dimension(1000, 520); // for the old screen
        var d = new Dimension(1016, 855); // for the new 110x44 screen
        f.setResizable(true);
        f.setSize(d);
        //f.setMaximumSize(d);
        //f.setMinimumSize(d);

        f.setVisible(true);
        f.setFocusable(true);
        f.requestFocus();
        f.setAutoRequestFocus(true);
        var console = new Console(textPane);
        f.addKeyListener(console);
        textPane.addKeyListener(console);

        return console;
    }

    private int lastLineLength = 0;

    public void printInline(String inStr) {
        var d = textPane.getStyledDocument();
        try {
            var sb = new StringBuilder(inStr.length() + 20);
            int lineLen = lastLineLength;
            for (int i = 0; i < inStr.length(); i++) {
                var chr = inStr.charAt(i);
                switch (chr) {
                    case '\b' -> {
                        if (!sb.isEmpty()) {
                            lineLen--;
                            sb.deleteCharAt(sb.length() - 1); // todo this is a hack and breaks for \t\b
                        } else {
                            lineLen = (lineLen + 3) % 4;
                            d.remove(d.getLength() - 1, 1);
                        }
                    }
                    case '\n' -> {
                        lineLen = 0;
                        sb.append(chr);
                    }
                    case '\t' -> {
                        var padLen = (4 - lineLen % 4);
                        sb.append(" ".repeat(padLen));
                        lineLen += padLen;
                        assert lineLen % 4 == 0;
                    }
                    default -> {
                        lineLen++;
                        sb.append(chr);
                    }
                }
            }
            d.insertString(d.getLength(), sb.toString(), null);
            lastLineLength = 0;
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public void println(String inStr) {
        printInline(inStr + "\n");
    }

    public void clear() {
        var d = textPane.getStyledDocument();
        try {
            d.remove(0, d.getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (this.onKeyTyped != null)
            this.onKeyTyped.accept(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (this.onKeyPressed != null)
            this.onKeyPressed.accept(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (this.onKeyReleased != null)
            this.onKeyReleased.accept(e);
    }

    String lastContent = null;

    public void drawTextBuffer(TextBufferUD buf) {
        var newContent = buf.getTextAsString();
        if (!newContent.equals(lastContent)) {
            lastContent = newContent;
            var d = textPane.getStyledDocument();
            textPane.select(0, 0);
            try {
                d.remove(0, d.getLength());
                d.insertString(0, newContent, null);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
