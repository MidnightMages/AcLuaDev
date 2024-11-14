package AcLuaDev;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class SandboxedFs {
    private final DirectoryNode root = new DirectoryNode(null);
//    private final HashMap<String, RandomAccessFile> files;
//    private final HashMap<String, String[]> directories;

    public SandboxedFs() {

    }

    public void init(Path path, DirectoryNode currentVirtualDir) {
        try (var stream = Files.walk(path, 0)) {
            stream.forEach(e -> {
                var name = e.getFileName().toString();
                if (Files.isRegularFile(e)) {
                    try {
                        currentVirtualDir.files.put(name, new RandomAccessFile(e.toFile(), "rw"));
                    }
                    catch (FileNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                } else if (Files.isDirectory(e)) {
                    var newDirNode = new DirectoryNode(name);
                    currentVirtualDir.childDirs.add(newDirNode);
                    init(e, newDirNode);
                } else {
                    throw new RuntimeException("unknown fs element type");
                }
            });
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
