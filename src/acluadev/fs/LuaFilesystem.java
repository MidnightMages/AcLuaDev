package acluadev.fs;

import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.functions.MixedStateFunctionRegistry;
import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.util.stream.Stream;

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
            return LuaFsFile.createAsUserdata(fs.getOrCreateFile(filename.asString()));
        }));
        reg.register("list", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            var files = fs.getFilesInDirectory(path.getString());
            var dirs = fs.getDirectoriesInDirectory(path.getString());
            return LuaObject.tableFromArray(Stream.concat(files.stream(), dirs.stream().map(x->x+"/")).map(LuaObject::of).toArray(LuaObject[]::new));
        }));
        reg.register("fileExists", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            return LuaObject.of(fs.getFile(path.getString()) != null);
        }));
        reg.register("directoryExists", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            return LuaObject.of(fs.getDirectory(path.getString()) != null);
        }));
        reg.register("delete", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            if (!fs.fileExists(path.asString())) {
                vm.error(LuaObject.of("File '%s' does not exist".formatted(path.asString())));
                return null;
            }
            return LuaObject.of(fs.tryDeleteFile(path.asString()));
        }));
        reg.register("copy", AtomicLuaFunction.forZeroResults(reg, (vm, disk, pathA, pathB) -> {
            var fs = getFsFromDisk(disk);
            if (!fs.fileExists(pathA.asString())) {
                vm.error(LuaObject.of("File '%s' does not exist".formatted(pathA.asString())));
                return;
            }
            fs.getOrCreateFile(pathB.asString()).writeAllText(fs.getFile(pathA.asString()).readAllText());
        }));
        reg.register("move", AtomicLuaFunction.forZeroResults(reg, (vm, disk, pathA, pathB) -> {
            var fs = getFsFromDisk(disk);
            if (!fs.fileExists(pathA.asString())) {
                vm.error(LuaObject.of("File '%s' does not exist".formatted(pathA.asString())));
                return;
            }
            fs.getOrCreateFile(pathB.asString()).writeAllText(fs.getFile(pathA.asString()).readAllText());
            if(!fs.tryDeleteFile(pathA.asString())) {
                throw new IllegalStateException("somehow file deletion failed after copying");
            }
        }));
        reg.register("getSize", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            if (!fs.fileExists(path.asString())) {
                vm.error(LuaObject.of("File '%s' does not exist".formatted(path.asString())));
                return null;
            }
            return LuaObject.of(fs.getFile(path.asString()).readAllText().length());
        }));
    }
}
