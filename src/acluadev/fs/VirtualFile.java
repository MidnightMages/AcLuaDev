package acluadev.fs;

import acluadev.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VirtualFile {
    private String content;
    private final DirectoryNode parentFolder;
    private final String fileName;

    public VirtualFile(String content, DirectoryNode parentFolder, String fileName) {
        this.content = content;
        this.parentFolder = parentFolder;
        this.fileName = fileName;
    }

    public static VirtualFile fromDiskFile(Path p, DirectoryNode parentFolder, String fileName) {
        try {
            var s = Files.readString(p);
            return new VirtualFile(s, parentFolder, fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getRealDiskPath() {
        var path = parentFolder.getRealDiskPath().resolve(this.fileName);
        var rootDir = parentFolder.getFsRootPath();
        if (!path.toAbsolutePath().startsWith(rootDir.toAbsolutePath()))
            throw new RuntimeException("Why are we trying to write outside of our root path?");
        return path;
    }

    public void writeContentsToDisk() {
        if (!parentFolder.isPhysReadOnly) {
            try {
                Main.SuppressAutoReload();
                var realDiskPath = getRealDiskPath();
                Files.createDirectories(realDiskPath.getParent());
                Files.writeString(realDiskPath, content);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void writeAllText(String s) {
        content = s;
        writeContentsToDisk();
    }

    public void appendAllText(String s) {
        content += s;
        writeContentsToDisk();
    }

    public String readAllText() {
        return content;
    }
}
