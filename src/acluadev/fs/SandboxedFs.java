package acluadev.fs;

import java.nio.file.Path;

public class SandboxedFs {
    private final DirectoryNode root = new DirectoryNode(null);
//    private final HashMap<String, RandomAccessFile> files;
//    private final HashMap<String, String[]> directories;

    public SandboxedFs() {

    }

    public void init(Path path) {
        root.init(path);
    }

    public VirtualFile getFile(String s) {
        return root.getFile(s.startsWith("/") ? s.substring(1) : s);
    }
}
