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

    public Collection<VirtualFile> getFilesInDirectory(String path) {
        return root.getDirectory(trimPath(path)).files.values();
    }

    public boolean fileExists(String path) {
        return root.fileExists(trimPath(path));
    }
}
