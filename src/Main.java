import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.internals.LuaVM_RT;

import java.io.IOException;
import java.nio.file.*;

import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE;
import static java.nio.file.StandardWatchEventKinds.*;

public class Main {

    private static void println(String s) {
        System.out.println(s);
    }

    private static LuaVM_RT startLuaVm(Path luaRootDir) {
        var rv = LuaVM.create().withStdLib();
        // todo inject globals, load main file, initialize readonly filesystem, run on new thread
        return (LuaVM_RT)rv;
    }

    private static void stopLuaVm(LuaVM_RT vm) {
        // todo kill the vm
    }

    public static void main(String[] args) throws IOException {
        var fs = FileSystems.getDefault();
        var watchPath = Path.of(System.getProperty("user.dir"), "luaRootReadOnly");
        if (!Files.exists(watchPath)) {
            println(("ERROR: Lua root path was determined as '%s', but this path does not exist. " +
                     "Please create this folder manually if necessary.").formatted(watchPath.toString()));
            System.exit(1);
        }

        while (true) {
            try (WatchService ws = fs.newWatchService()) {
                watchPath.register(ws, new WatchEvent.Kind[]{ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE}, FILE_TREE);
                LuaVM_RT lvm = startLuaVm(watchPath);
                ws.take();
                try {
                    stopLuaVm(lvm);
                } catch (Exception ex) {
                    println(ex.toString());
                }
            }
            catch (InterruptedException e) {
                println("Interrupted");
                break;
            }
        }

        println("done");
    }
}