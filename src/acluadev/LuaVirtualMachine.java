package acluadev;

import acluadev.fs.SandboxedFs;
import acluadev.misc.*;
import dev.asdf00.jluavm.LuaVM;
import dev.asdf00.jluavm.api.functions.ApiFunctionRegistry;
import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.functions.MixedStateFunctionRegistry;
import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class LuaVirtualMachine {
    public boolean enableBeep;
    public ComputerUD luaComputer;

    public void dirtyBuffer(TextBufferUD buf) {
        buf.getAssociatedScreens().forEach(screen -> {
            screen.drawTextBuffer(buf);
        });
    }

    public void triggerMachineEvent(String eventName, LuaObject... args) {
        luaComputer.triggerMachineEvent(eventName, args);
    }

    public void startLuaVm(Path luaRootDir, Config cfg, ScreenBlockEntity[] screenConsoles) {
        enableBeep = cfg.enableComputerBeep();

        String bootFile; // read uefi file
        try {
            bootFile = Files.readString(luaRootDir.resolve("uefi.lua"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        luaComputer = new ComputerUD(new UefiUD(bootFile), new NvramUD());

        // REGISTER USERDATA COMPONENTS
        var componentReg = new ComponentRegistryUD(this);
        // set up disk filesystems
        for (int i = 1; i <= 3; i++) {
            var dp = luaRootDir.resolve("disk" + i);
            var fs = new SandboxedFs(dp, !cfg.allowPhysicalFilesystemWrites());
            try {
                if (!Files.isDirectory(dp))
                    Files.createDirectory(dp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            fs.init(dp);

            var ud = new ManagedMassStorageUD("disk",i ,fs);
            componentReg.addComponentInitAndNotify(ud);
        }
        componentReg.addComponentInitAndNotify(luaComputer);
        componentReg.addComponentInitAndNotify(new InternetUD());
        componentReg.addComponentInitAndNotify(new GpuUD());

        Arrays.stream(screenConsoles).forEach(x-> {
            componentReg.addComponentInitAndNotify(new ScreenUD(x));
        });

        // SET UP GLOBAL ENV
        var _G = LuaObject.table();

        // ADD COMPONENT TO _G
        _G.set("components", LuaObject.of(componentReg));
        _G.set("_HOST", LuaObject.of("Advanced Computers Test Harness"));
        _G.set("print", LuaObject.of(BUILTIN_FUNCTIONS.getFunction("print")));
        _G.set("printInline", LuaObject.of(BUILTIN_FUNCTIONS.getFunction("printInline")));
        _G.set("sleep", LuaObject.of(BUILTIN_FUNCTIONS.getFunction("sleep")));
        var vm = loadMeasured(_G, bootFile);
        println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"); // as good of a Console.Clear(); as we are gonna get :C
        println("============ EXECUTING ============");
        var res = vm.run();
        println("============== DONE ============");
        println("RESULT: " + res.state().toString() + "; " + Arrays.stream(res.returnVars()).map(Object::toString).collect(Collectors.joining()));
    }

    private static LuaVM loadMeasured(LuaObject env, String code) {
        println("Loading");
        var n = System.nanoTime();
        var rv = LuaVM.builder().withApiRegistry(BUILTIN_FUNCTIONS).modifyEnv(t -> {
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

    public static final MixedStateFunctionRegistry BUILTIN_FUNCTIONS;

    static {
        BUILTIN_FUNCTIONS = new MixedStateFunctionRegistry("advancedcomputers.builtins");
        BUILTIN_FUNCTIONS.register("print",
                AtomicLuaFunction.vaForZeroResults(BUILTIN_FUNCTIONS, (vm, args) ->
                        printlnLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));
        BUILTIN_FUNCTIONS.register("printInline",
                AtomicLuaFunction.vaForZeroResults(BUILTIN_FUNCTIONS, (vm, args) ->
                        printInlineLUA(Arrays.stream(args).map(LuaObject::asString).collect(Collectors.joining("\t")))));
        BUILTIN_FUNCTIONS.register("sleep",
                AtomicLuaFunction.forZeroResults(BUILTIN_FUNCTIONS, (vm, arg) -> {
                            try {
                                Thread.sleep((int) (arg.asDouble() * 1000f));
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ));

        // general purpose iterator that returns one set of values after another
        BUILTIN_FUNCTIONS.register("$internal.unpacking_iterator", LuaUnpackingIteratorFunction.class,
                (tableToIterateOver, closures) -> new LuaUnpackingIteratorFunction(BUILTIN_FUNCTIONS, tableToIterateOver, closures));
    }

    // ------------
    private static void println(String s) {
        System.out.println(s);
    }

    private static void printlnLUA(String s) {
        println(s);
    }

    private static void printInlineLUA(String s) {
        System.out.print(s);
    }

    // ------------
}
