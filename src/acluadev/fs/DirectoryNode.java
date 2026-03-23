package acluadev.fs;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class DirectoryNode {
    final String nameOrPath;
    private final DirectoryNode parentFolder;
    final boolean isPhysReadOnly;
    final HashMap<String, DirectoryNode> childDirs = new HashMap<>();
    final HashMap<String, VirtualFile> files = new HashMap<>();

    public DirectoryNode(String nameOrPath, DirectoryNode parentFolder, boolean isPhysReadOnly) {
        this.nameOrPath = nameOrPath;
        this.parentFolder = parentFolder;
        this.isPhysReadOnly = isPhysReadOnly;
    }

    public VirtualFile getFileOrNull(String s) {
        var splitted = s.split("/", 2);
        return splitted.length == 1 ? files.get(splitted[0]) : Optional.ofNullable(childDirs.get(splitted[0])).map(x -> x.getFileOrNull(splitted[1])).orElse(null);
    }

    public VirtualFile getOrCreateFile(String s) {
        var splitted = s.split("/", 2);
        var fileObject = splitted.length == 1 ?
                files.get(splitted[0]) :
                Optional.ofNullable(childDirs.get(splitted[0])).map(x -> x.getOrCreateFile(splitted[1])).orElse(null);
        if (fileObject == null) {
            fileObject = new VirtualFile("", this, splitted[0]);
            fileObject.writeContentsToDisk();
            this.files.put(splitted[0], fileObject);
        }
        return fileObject;
    }

    public boolean tryDeleteFile(String s) {
        var splitted = s.split("/", 2);
        return splitted.length == 1 ? files.remove(splitted[0]) != null : childDirs.get(splitted[0]).tryDeleteFile(splitted[1]);
    }

    public DirectoryNode getDirectory(String s) {
        if (s.isEmpty())
            return this;

        var splitted = s.split("/", 2);
        return splitted.length == 1 ? childDirs.get(splitted[0]) : Optional.ofNullable(childDirs.get(splitted[0])).map(x -> x.getDirectory(splitted[1])).orElse(null);
    }

    public DirectoryNode createDirectoryAndParents(String s) {
        if (s.isEmpty())
            return this;

        var splitted = s.split("/", 2);
        var childDir = childDirs.getOrDefault(splitted[0], null);
        if (childDir == null) {
            childDir = new DirectoryNode(splitted[0], this, isPhysReadOnly);
            childDirs.put(splitted[0], childDir);
        }
        return splitted.length == 1 ? childDir : childDir.createDirectoryAndParents(splitted[1]);
    }

    public DirectoryNode addChildDir(String name) {
        var newDirNode = new DirectoryNode(name, this, isPhysReadOnly);
        this.childDirs.put(name, newDirNode);
        return newDirNode;
    }

    public void init(Path path) {
        try (var stream = Files.walk(path, 1, FileVisitOption.FOLLOW_LINKS)) {
            stream.forEach(e -> {
                if (e.compareTo(path) == 0)
                    return;

                var name = e.getFileName().toString();
                if (Files.isRegularFile(e)) {
                    files.put(name, VirtualFile.fromDiskFile(e, this, name));
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

    public Path getRealDiskPath() {
        if (parentFolder == null)
            return Path.of(this.nameOrPath);

        return parentFolder.getRealDiskPath().resolve(this.nameOrPath);
    }

    public Path getFsRootPath() {
        return parentFolder == null ? Path.of(this.nameOrPath) : parentFolder.getFsRootPath();
    }
}
