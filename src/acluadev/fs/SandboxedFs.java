package acluadev.fs;

import java.nio.file.Path;
import java.util.Collection;

public class SandboxedFs {
    private final DirectoryNode root = new DirectoryNode(null);
//    private final HashMap<String, RandomAccessFile> files;
//    private final HashMap<String, String[]> directories;

    public SandboxedFs() {

    }

    public void init(Path path) {
        root.init(path);
    }

    private static String trimPath(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }
    public VirtualFile getFile(String s) {
        return root.getFile(trimPath(s));
    }

    public Collection<String> getFilesInDirectory(String path) {
        return root.getDirectory(trimPath(path)).files.keySet();
    }

    public boolean fileExists(String path) {
        return root.fileExists(trimPath(path));
    }

    public boolean tryDeleteFile(String s) {
        return root.tryDeleteFile(trimPath(s));
    }
}
