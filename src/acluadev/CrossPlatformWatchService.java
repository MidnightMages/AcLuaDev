package acluadev;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class CrossPlatformWatchService {
    public static void waitForChange(Path rootDir) throws InterruptedException {
        var fs = FileSystems.getDefault();
        try (WatchService ws = fs.newWatchService()) {
            try (var w = Files.walk(rootDir, FileVisitOption.FOLLOW_LINKS)) {
                for (var dir : w.filter(Files::isDirectory).toArray(Path[]::new)) {
                    dir.register(ws, new WatchEvent.Kind[]{ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE}, SensitivityWatchEventModifier.HIGH);
                }
            }
            ws.take();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
