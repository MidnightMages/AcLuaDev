package acluadev.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VirtualFile {
    private String content;

    public VirtualFile(String content) {
        this.content = content;
    }

    public static VirtualFile fromDiskFile(Path p) {
        try {
            var s = Files.readString(p);
            return new VirtualFile(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeAllText(String s) {
        content = s;
    }

    public void appendAllText(String s) {
        content += s;
    }

    public String readAllText() {
        return content;
    }
}
