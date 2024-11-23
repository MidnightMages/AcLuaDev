package acluadev;

import acluadev.fs.LuaFilesystem;
import acluadev.fs.SandboxedFs;
import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.AtomicLuaFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE;
import static java.nio.file.StandardWatchEventKinds.*;

public class Main {

    private static void println(String s) {
        System.out.println(s);
    }

    private static void loadMeasured(LuaVM vm, String code) {
        println("Loading");
        var n = System.nanoTime();
        vm.withRootFunc(code);
        var n2 = System.nanoTime();
        var delta = n2 - n;
        println("Load finished in %.3f s".formatted(delta / 1000_000_000d));
    }

    private static LuaVM_RT startLuaVm(Path luaRootDir) {
        var fs = new SandboxedFs();
        fs.init(luaRootDir);
        var f = fs.getFile("/boot.lua");
        var rv = LuaVM.create().withStdLib();
        var _G = rv.get_G();
        _G.set("fs", new LuaFilesystem(fs).getTable());
        _G.set("print", AtomicLuaFunction.vaForZeroResults((vm, args) -> println(Arrays.stream(args[0].asArray()).map(LuaObject::asString).collect(Collectors.joining("\t")))).obj());

        var br = new BufferedReader(new InputStreamReader(System.in));
        _G.set("readline", AtomicLuaFunction.forOneResult((vm, msg) -> {
            try {
                if (!msg.isNil()){
                    println(msg.asString());
                }

                return LuaObject.of(br.readLine());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).obj());
        // todo inject globals, load main file, initialize readonly filesystem, run on new thread
        for (int i = 0; i < 1; i++) {
            loadMeasured(rv, f.readAllText());
        }
        println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"); // as good of a Console.Clear(); as we are gonna get :C
        println("============ EXECUTING ============");
        var res = rv.run();
        println("============== DONE ============");
        println("RESULT: " + res.state().toString() + "; " + Arrays.stream(res.returnVars()).map(Object::toString).collect(Collectors.joining()));
        return (LuaVM_RT) rv;
    }

    private static void stopLuaVm(LuaVM_RT vm) {
        // todo kill the vm
    }

    @SuppressWarnings("BusyWait")
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
                }
                catch (Exception ex) {
                    println(ex.toString());
                }
                Thread.sleep(250);
            }
            catch (InterruptedException e) {
                println("Interrupted");
                break;
            }
        }

        println("done");
    }
}