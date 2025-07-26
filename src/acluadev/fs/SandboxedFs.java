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
        while (s.endsWith("/"))
            s = s.substring(0, s.length() - 1);
        return s.startsWith("/") ? s.substring(1) : s;
    }

    public VirtualFile getFile(String s) {
        return root.getFile(trimPath(s));
    }

    public VirtualFile getOrCreateFile(String s) {
        return root.getOrCreateFile(trimPath(s));
    }

    public DirectoryNode getDirectory(String s) {
        return root.getDirectory(trimPath(s));
    }

    public DirectoryNode createDirectoryAndParents(String s) {
        return root.createDirectoryAndParents(trimPath(s));
    }

    public Collection<String> getFilesInDirectory(String path) {
        return root.getDirectory(trimPath(path)).files.keySet();
    }

    public Collection<String> getDirectoriesInDirectory(String path) {
        return root.getDirectory(trimPath(path)).childDirs.keySet();
    }

    public boolean fileExists(String path) {
        return getFile(path) != null;
    }

    public boolean tryDeleteFile(String s) {
        return root.tryDeleteFile(trimPath(s));
    }
}
