package acluadev.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class DirectoryNode {
    final String name;
    final HashMap<String, DirectoryNode> childDirs = new HashMap<>();
    final HashMap<String, VirtualFile> files = new HashMap<>();

    public DirectoryNode(String name) {
        this.name = name;
    }

    public VirtualFile getFile(String s) {
        var splitted = s.split("/", 2);
        return splitted.length == 1 ? files.get(splitted[0]) : childDirs.get(splitted[0]).getFile(splitted[1]);
    }

    public boolean tryDeleteFile(String s) {
        var splitted = s.split("/", 2);
        return splitted.length == 1 ? files.remove(splitted[0]) != null : childDirs.get(splitted[0]).tryDeleteFile(splitted[1]);
    }

    public DirectoryNode getDirectory(String s) {
        var splitted = s.split("/", 2);
        return splitted.length == 1 ? childDirs.get(splitted[0]) : childDirs.get(splitted[0]).getDirectory(splitted[1]);
    }

    public boolean fileExists(String s) {
        var splitted = s.split("/", 2);
        return splitted.length == 1 ? files.containsKey(splitted[0]) : (childDirs.containsKey(splitted[0]) && childDirs.get(splitted[0]).fileExists(splitted[1]));
    }

    public DirectoryNode addChildDir(String name) {
        var newDirNode = new DirectoryNode(name);
        this.childDirs.put(name, newDirNode);
        return newDirNode;
    }

    public void init(Path path) {
        try (var stream = Files.walk(path, 1)) {
            stream.forEach(e -> {
                if (e.compareTo(path) == 0)
                    return;

                var name = e.getFileName().toString();
                if (Files.isRegularFile(e)) {
                    files.put(name, VirtualFile.fromDiskFile(e));
                } else if (Files.isDirectory(e)) {
                    if (!e.endsWith("/.vscode/"))
                        addChildDir(name).init(e);
                } else {
                    throw new RuntimeException("unknown fs element type");
                }
            });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
