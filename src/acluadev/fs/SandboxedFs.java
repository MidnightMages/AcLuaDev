package acluadev.fs;

import dev.asdf00.jluavm.exceptions.LuaJavaError;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

public class SandboxedFs {
    private final DirectoryNode root;
//    private final HashMap<String, RandomAccessFile> files;
//    private final HashMap<String, String[]> directories;

    public SandboxedFs(Path rootPath, boolean isReadOnly) {
        root = new DirectoryNode(rootPath.toString(), null, isReadOnly);
    }

    public void init(Path path) {
        root.init(path);
    }

    private static String trimPath(String s) {
        while (s.endsWith("/"))
            s = s.substring(0, s.length() - 1);
        return s.startsWith("/") ? s.substring(1) : s;
    }

    public VirtualFile getFileOrNull(String s) {
        return root.getFileOrNull(trimPath(s));
    }

    public VirtualFile getOrCreateFile(String s) {
        return root.getOrCreateFile(trimPath(s));
    }

    public DirectoryNode getDirectory(String s) {
        return root.getDirectory(trimPath(s));
    }

    public boolean directoryExists(String s) {
        return getDirectory(s) != null;
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
        return getFileOrNull(path) != null;
    }

    public boolean tryDeleteFile(String s) {
        return root.tryDeleteFile(trimPath(s));
    }

    public boolean tryDeleteDirectoryRecursively(String s) {
        var d = root.getDirectory(trimPath(s));
        if (d != null) {
            d.deleteChildFoldersAndSelf();
            return true;
        }
        return false;
    }
}
