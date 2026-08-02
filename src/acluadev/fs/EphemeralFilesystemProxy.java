package acluadev.fs;

import acluadev.misc.RuntimeAssert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;

public class EphemeralFilesystemProxy {
    /**
     * All actions must be in this sub-folder, otherwise we throw an exception
     */
    private final Path realFsBasePathForAsserts;
    private final boolean isEphemeral;
    private final HashMap<Path, String> modifiedFileContents; // contains newly added virtual files
    private final HashSet<Path> deletedFiles; // should contain all files that virtually no longer exist in the filesystem
    private final HashSet<Path> deletedDirectories; // same but for folders
    private final HashSet<Path> createdDirectories; // created virtual folders

    public EphemeralFilesystemProxy(Path realFsBasePathForAsserts, boolean isEphemeral) {
        this.realFsBasePathForAsserts = realFsBasePathForAsserts;
        this.isEphemeral = isEphemeral;
        if (isEphemeral) {
            modifiedFileContents = new HashMap<>();
            deletedFiles = new HashSet<>();
            deletedDirectories = new HashSet<>();
            createdDirectories = new HashSet<>();
        } else {
            modifiedFileContents = null;
            deletedFiles = null;
            deletedDirectories = null;
            createdDirectories = null;
        }
    }

    private void assertPathIsValid(Path p) {
        RuntimeAssert.RuntimeAssert(p.startsWith(realFsBasePathForAsserts), "path is not locaed in our filesystem");
    }

    private void assertPathNotInDeletedFolders(Path p) {
        assertPathIsValid(p);
        assert deletedDirectories != null;
        for (var inaccessiblePath : deletedDirectories) {
            RuntimeAssert.RuntimeAssert(!p.startsWith(inaccessiblePath), "path is located in directory that does not exist anymore, virtually.");
        }
    }

    public boolean exists(Path p) {
        assertPathIsValid(p);
        if (!isEphemeral)
            return Files.exists(p);

        // if ephemeral
        assert deletedFiles != null;
        assert modifiedFileContents != null;
        if (deletedFiles.contains(p)) {
            RuntimeAssert.RuntimeAssert(!modifiedFileContents.containsKey(p), "cleanup wasnt done properly");
            return false;
        }
        return modifiedFileContents.containsKey(p) || Files.exists(p);
    }

    public boolean isRegularFile(Path p) {
        assertPathIsValid(p);
        if (isEphemeral) {
            assert modifiedFileContents != null;
            if (modifiedFileContents.containsKey(p)) {
                assertPathNotInDeletedFolders(p);
                return true;
            }
        }
        return Files.isRegularFile(p);
    }

    public boolean isDirectory(Path p) {
        assertPathIsValid(p);
        if (isEphemeral) {
            assert deletedDirectories != null;
            if (deletedDirectories.contains(p))
                return false;
        }
        return Files.isDirectory(p);
    }

    public String readString(Path p) throws IOException {
        assertPathIsValid(p);
        RuntimeAssert.RuntimeAssert(exists(p), "file didnt exist somehow");
        if (isEphemeral) {
            assert deletedFiles != null;
            assert modifiedFileContents != null;
            var contents = modifiedFileContents.get(p);
            if (contents != null) {
                RuntimeAssert.RuntimeAssert(!deletedFiles.contains(p), "cleanup wasnt done properly");
                assertPathNotInDeletedFolders(p);
                return contents;
            }
            // otherwise try reading
        }
        return Files.readString(p);
    }

    public void writeString(Path p, CharSequence content) throws IOException {
        assertPathIsValid(p);
        RuntimeAssert.RuntimeAssert(exists(p), "file didnt exist somehow");
        if (isEphemeral) {
            assert deletedFiles != null;
            assert modifiedFileContents != null;
            RuntimeAssert.RuntimeAssert(!deletedFiles.contains(p), "file is still marked as deleted");
            assertPathNotInDeletedFolders(p);
            modifiedFileContents.put(p, content.toString());
        } else {
            Files.writeString(p, content);
        }
    }

    public void createFile(Path p) throws IOException {
        assertPathIsValid(p);
        if (isEphemeral) {
            assertPathNotInDeletedFolders(p);
            assert modifiedFileContents != null;
            RuntimeAssert.RuntimeAssert(!modifiedFileContents.containsKey(p), "file exists already???");
            modifiedFileContents.put(p, "");
        } else {
            Files.createFile(p);
        }
    }

    public void delete(Path p) throws IOException {
        assertPathIsValid(p);
        if (isEphemeral) {
            if (isDirectory(p)) {
                assert deletedDirectories != null;
                if (!deletedDirectories.add(p))
                    throw new NoSuchFileException("already deleted");

                assert deletedFiles != null;
                try (var f = list(p)) { // make sure there exist no more files in this folder
                    f.forEach(x -> {
                        if (Files.isRegularFile(x)) {
                            RuntimeAssert.RuntimeAssert(deletedFiles.contains(x), "directory was not empty");
                        } else if (Files.isDirectory(x)) {
                            RuntimeAssert.RuntimeAssert(deletedDirectories.contains(x), "directory was not empty");
                        }
                    });
                }
            } else if (isRegularFile(p)) {
                assert deletedDirectories != null;
            } else {
                throw new NoSuchFileException("not found");
            }
        } else {
            Files.delete(p);
        }
    }

    public Stream<Path> walk(Path p, int depth) throws IOException {
        RuntimeAssert.RuntimeAssert(depth == Integer.MAX_VALUE, "only max depth supported for now");
        assertPathIsValid(p);
        var stream = Files.walk(p, depth);
        if (isEphemeral) {
            assert deletedFiles != null;
            assert deletedDirectories != null;
            assert modifiedFileContents != null;
            stream = Stream.concat(stream, modifiedFileContents
                    .keySet()
                    .stream()
                    .filter(x -> x.subpath(0, x.getNameCount() - 1).startsWith(p)));
            stream = stream.filter(x -> !deletedFiles.contains(x) && deletedDirectories.stream().noneMatch(x::startsWith));
        }
        return stream;
    }

    public Stream<Path> list(Path p) throws IOException {
        assertPathIsValid(p);
        var stream = Files.list(p);
        if (isEphemeral) {
            assert deletedFiles != null;
            assert deletedDirectories != null;
            assert modifiedFileContents != null;
            assert createdDirectories != null;
            stream = Stream.concat(stream, Stream.concat(modifiedFileContents.keySet().stream(), createdDirectories.stream())
                    .filter(x -> x.subpath(0, x.getNameCount() - 1).equals(p)));
            stream = stream.filter(x -> !deletedFiles.contains(x) && !deletedDirectories.contains(x));
        }
        return stream;
    }

    public void move(Path p1, Path p2) throws IOException {
        assertPathIsValid(p1);
        assertPathIsValid(p2);
        if (isEphemeral) {
            if (isRegularFile(p1)) {
                RuntimeAssert.RuntimeAssert(!exists(p2), "dest already exists");
                RuntimeAssert.RuntimeAssert(exists(p2.getParent()), "parent does not exist");
                assert modifiedFileContents != null;
                modifiedFileContents.put(p2, readString(p1));
                delete(p1);
            } else if (isDirectory(p1)) {
                try (var entriesToMove = list(p1)) {
                    entriesToMove.forEach(srcPath -> {
                        RuntimeAssert.RuntimeAssert(srcPath.getParent().equals(p1), "somehow this source is not located in p1?");
                        try {
                            move(srcPath, p2.resolve(p1.getName(p1.getNameCount() - 1)));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } else {
                throw new IOException("src wasnt found");
            }
        } else {
            Files.move(p1, p2);
        }
    }

    public void createDirectories(Path p) throws IOException {
        assertPathIsValid(p);
        if (isEphemeral) {
            for (int i = 0; i < p.getNameCount(); i++) {
                var currPath = p.subpath(0, i);
                if (isRegularFile(currPath))
                    throw new IOException("parent is a file?");

                if (!isDirectory(currPath)) {
                    assert createdDirectories != null;
                    assert deletedDirectories != null;
                    createdDirectories.add(currPath);
                    deletedDirectories.remove(currPath);
                }
            }
        } else {
            Files.createDirectories(p);
        }
    }

    public long size(Path p) throws IOException {
        assertPathIsValid(p);
        if (!isEphemeral)
            return Files.size(p);

        RuntimeAssert.RuntimeAssert(exists(p), "file does not exist");
        return readString(p).length();
    }
}
