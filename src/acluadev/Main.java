package acluadev;

import acluadev.misc.ScreenBlockEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.asdf00.jluavm.api.functions.ApiFunctionRegistry;
import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.internals.javac.DelayedJavaCompiler;
import dev.asdf00.jluavm.runtime.types.LuaJavaApiFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Main {
    private final static long fileWriteReloadSuppressDurationMs = 1000;
    private static volatile long autoReloadDisabledUntil = System.currentTimeMillis();

    public static LuaJavaApiFunction createIterFunction(Supplier<LuaObject[][]> retsS) {
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
        println(s);
    }

    private static void printInlineLUA(String s) {
        System.out.print(s);
    }


    @SuppressWarnings("BusyWait")
    public static void main(String[] args) throws IOException {
        var projDir = System.getProperty("user.dir");
        var configPath = Path.of(projDir, "config.json");
        if (!Files.exists(configPath)) {
            Files.writeString(configPath, new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(
                    new Config("lua/AdvancedOS", false,
                            false, 1)
                    )
            );
        }
        var cfg = new ObjectMapper().readValue(Files.readString(configPath), Config.class);
        var watchPath = Path.of(projDir, cfg.luaRootDirectory());
        if (!Files.exists(watchPath)) {
            println(("ERROR: Lua root path was determined as '%s', but this path does not exist. " +
                     "Please create this folder manually if necessary.").formatted(watchPath.toString()));
            System.exit(1);
        }

        // persists across vms
        ScreenBlockEntity[] screenConsoles = Stream.generate(ScreenBlockEntity::new)
                .limit(cfg.screenCount())
                .toArray(ScreenBlockEntity[]::new);
        Arrays.stream(screenConsoles).forEach(x->
                x.create());
        while (true) {

            LuaVirtualMachine vm = new LuaVirtualMachine();
            Arrays.stream(screenConsoles).forEach(x->x.clear());
            Arrays.stream(screenConsoles).forEach(x->x.init(vm));
            Thread lvmThread = new Thread(() -> {
                vm.startLuaVm(watchPath, cfg, screenConsoles);
            });
            lvmThread.start();
            try {
                do {
                    CrossPlatformWatchService.waitForChange(watchPath);
                } while (System.currentTimeMillis() < autoReloadDisabledUntil);

            } catch (InterruptedException e2) {
                println("Interrupted");
                break;
            }
            try {
                vm.triggerMachineEvent("shutdown");
                try {
                    lvmThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception ex) {
                println(ex.toString());
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        println("done");
    }

    public static void SuppressAutoReload() {
        autoReloadDisabledUntil = System.currentTimeMillis() + fileWriteReloadSuppressDurationMs;
    }
}