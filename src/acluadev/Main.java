package acluadev;

import acluadev.fs.LuaFilesystem;
import acluadev.fs.SandboxedFs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.api.functions.ApiFunctionRegistry;
import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.functions.LuaJavaApiFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE;
import static java.nio.file.StandardWatchEventKinds.*;

public class Main {

    private static Console console;
    private static AcEventQueue eventQueue;
    private static Thread lvmThread;

    private static LuaJavaApiFunction createIterFunction(Supplier<LuaObject[][]> retsS) {
        ApiFunctionRegistry reg = null;
        return AtomicLuaFunction.forManyResults(reg, $ -> {
            var rets = retsS.get();
            return new LuaObject[]{
                    AtomicLuaFunction.forManyResults(reg, (vm, state) -> {
                        var oldIdx = state.get(LuaObject.of(0));
                        if (!oldIdx.isLong()) {
                            vm.error(LuaObject.of("Internal error, or someone messed with the iterator state"));
                            return null;
                        }
                        int nuIdx = (int) oldIdx.asLong() + 1;
                        if (nuIdx < rets.length && nuIdx >= 0) {
                            state.set(LuaObject.of(0), LuaObject.of(nuIdx));
                            return rets[nuIdx];
                        } else {
                            return new LuaObject[0];
                        }
                    }).obj(),
                    LuaObject.table(LuaObject.of(0), LuaObject.of(-1))
            };
        });

    }

    private static void println(String s) {
        System.out.println(s);
    }

    private static void printlnLUA(String s) {
        console.println(s);
        println(s);
    }

    private static void printInlineLUA(String s) {
        console.printInline(s);
        System.out.print(s);
    }

    private static LuaVM loadMeasured(ApiFunctionRegistry reg, LuaObject env, String code) {
        println("Loading");
        var n = System.nanoTime();
        var rv = LuaVM.builder().withApiRegistry(reg).modifyEnv(t -> {
            var map = env.asMap();
            for (var k : map.keys()) {
                t.set(k, map.getOrDefault(k, LuaObject.NIL));
            }
        }).rootFunc(code).build();
        var n2 = System.nanoTime();
        var delta = n2 - n;
        println("Load finished in %.3f s".formatted(delta / 1000_000_000d));
        return rv;
    }

    private static LuaVM startLuaVm(Path luaRootDir) {
        String bootFile; // read bios file
        try {
            bootFile = String.join("\n", Files.readAllLines(luaRootDir.resolve("bios.lua")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        eventQueue = new AcEventQueue();
        console.onKeyPressed = eventQueue::addKeyPressed;
        console.onKeyReleased = eventQueue::addKeyReleased;
        console.onKeyTyped = eventQueue::addKeyTyped;

        var greg = new ScopedMixedStateFunctionRegistry("testHarness");
        var componentRegistry = greg.forTable("component");
        var computerRegistry = greg.forTable("computer");

        var allComponents = new ArrayList<LuaComponent>();
        var _G = LuaObject.table();

        var fss = new ArrayList<SandboxedFs>();
        var lfs = new LuaFilesystem();
        var fsReg = new ScopedMixedStateFunctionRegistry("testHarness_internal");
        lfs.registerFuncs(fsReg);
        // set up disk filesystems
        for (int i = 1; i <= 3; i++) {
            var fs = new SandboxedFs();
            var dp = luaRootDir.resolve("disk" + i);
            try {
                if (!Files.isDirectory(dp))
                    Files.createDirectory(dp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            fs.init(dp);
            fss.add(fs);

            var t = LuaObject.table();
            t.set("id", LuaObject.of("disk_" + i));
            t.set("__UDATA_id", LuaObject.of(i));
            fsReg.addFunctionsToTable(t);
            allComponents.add(new LuaComponent("disk", t));
        }
        lfs.init(fss.toArray(SandboxedFs[]::new));

        componentRegistry.register("list", createIterFunction(() -> allComponents.stream().map(LuaComponent::asLuaObj).toArray(LuaObject[][]::new)));
        computerRegistry.register("getMachineEvent", AtomicLuaFunction.forManyResults(greg, vm -> {
            var e = eventQueue.getQueuedEventOrNull();
            return e == null ? new LuaObject[]{LuaObject.NIL} : e;
        }));

        greg.register("sleep", AtomicLuaFunction.forZeroResults(greg, (vm, time) -> {
            try {
                Thread.sleep((int) (time.asDouble() * 1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }));
        greg.register("print",
                AtomicLuaFunction.vaForZeroResults(greg, (vm, args) -> printlnLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));
        greg.register("printInline",
                AtomicLuaFunction.vaForZeroResults(greg, (vm, args) -> printInlineLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));

        var br = new BufferedReader(new InputStreamReader(System.in));
        greg.register("readline", AtomicLuaFunction.forOneResult(greg, (vm, msg) -> {
            try {
                if (!msg.isNil()) {
                    printlnLUA(msg.asString());
                }

                return LuaObject.of(br.readLine());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        // todo inject globals, load main file, initialize readonly filesystem, run on new thread
        greg.addFunctionsToTable(_G);
        var vm = loadMeasured(greg, _G, bootFile);

        for (var comp : allComponents)
            eventQueue.addComponentAdded(comp);
        println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"); // as good of a Console.Clear(); as we are gonna get :C
        println("============ EXECUTING ============");
        var res = vm.run();
        println("============== DONE ============");
        println("RESULT: " + res.state().toString() + "; " + Arrays.stream(res.returnVars()).map(Object::toString).collect(Collectors.joining()));
        return vm;
    }

    private static void stopLuaVm() {
        eventQueue.addRequestShutdown();
        try {
            lvmThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("BusyWait")
    public static void main(String[] args) throws IOException {
        console = Console.createConsole();
        var fs = FileSystems.getDefault();
        var projDir = System.getProperty("user.dir");
        var configPath = Path.of(projDir, "config.json");
        if (!Files.exists(configPath)) {
            Files.writeString(configPath, new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(new Config("lua/AdvancedOS")));
        }
        var cfg = new ObjectMapper().readValue(Files.readString(configPath), Config.class);
        var watchPath = Path.of(projDir, cfg.luaRootDirectory());
        if (!Files.exists(watchPath)) {
            println(("ERROR: Lua root path was determined as '%s', but this path does not exist. " +
                     "Please create this folder manually if necessary.").formatted(watchPath.toString()));
            System.exit(1);
        }

        while (true) {
            try (WatchService ws = fs.newWatchService()) {
                watchPath.register(ws, new WatchEvent.Kind[]{ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE}, FILE_TREE);
                lvmThread = new Thread(() -> startLuaVm(watchPath));
                console.clear();
                lvmThread.start();
                ws.take();
                try {
                    stopLuaVm();
                } catch (Exception ex) {
                    println(ex.toString());
                }
                Thread.sleep(250);
            } catch (InterruptedException e) {
                println("Interrupted");
                break;
            }
        }

        println("done");
    }
}