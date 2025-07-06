package acluadev.fs;

import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.functions.MixedStateFunctionRegistry;
import dev.asdf00.jluavm.runtime.types.LuaObject;

public class LuaFilesystem {
    private SandboxedFs[] fss;

    public LuaFilesystem() {
    }

    public void init(SandboxedFs[] fileSystems) {
        if (fss != null)
            throw new RuntimeException("init was already called");
        fss = fileSystems;
    }

    private SandboxedFs getFsFromDisk(LuaObject disk) {
        return fss[((int) disk.get("__UDATA_id").asLong()) - 1];
    }

    // https://www.lua.org/manual/5.4/manual.html#6.8
    public void registerFuncs(MixedStateFunctionRegistry reg) {
        // TODO return a userdata filehandle table

        reg.register("open", AtomicLuaFunction.forOneResult(reg, (vm, disk, filename) -> {
            var fs = getFsFromDisk(disk);
            if (!fs.fileExists(filename.asString())) {
                vm.error(LuaObject.of("File '%s' does not exist".formatted(filename.asString())));
                return null;
            }
            return LuaFsFile.createAsUserdata(fs.getFile(filename.asString()));
        }));
        reg.register("list", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            return LuaObject.table(fs.getFilesInDirectory(path.getString()).stream().map(LuaObject::of).toArray(LuaObject[]::new));
        }));
        reg.register("fileExists", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            return LuaObject.of(fs.fileExists(path.getString()));
        }));
        reg.register("delete", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            if (!fs.fileExists(path.asString())) {
                vm.error(LuaObject.of("File '%s' does not exist".formatted(path.asString())));
                return null;
            }
            return LuaObject.of(fs.tryDeleteFile(path.asString()));
        }));
    }
}
