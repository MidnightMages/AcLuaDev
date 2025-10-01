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

        reg.register("open", AtomicLuaFunction.forOneResult(reg, (vm, disk, fileNameL, autoCreate) -> {
            var fs = getFsFromDisk(disk);
            if (!autoCreate.isBoolean() && !autoCreate.isNil()) {
                vm.error(LuaObject.of("Arg #2 must be bool or nil"));
                return null;
            }

            var fileName = fileNameL.asString();
            if (fileName.startsWith("/"))
                fileName = fileName.substring(1);

            if (fileName.endsWith("/")) {
                vm.error(LuaObject.of("Filename cannot end with a slash"));
                return null;
            }

            var fileExists = fs.fileExists(fileName);
            if (!fileExists) {
                if (!autoCreate.isTruthy()) { // if no autocreate and doesnt exist, then we throw an error
                    vm.error(LuaObject.of("File '%s' does not exist".formatted(fileName)));
                    return null;
                } else {// if we will be creating a new file, check the existence of the parent folder
                    var lastSlash = fileName.lastIndexOf('/');
                    if (lastSlash != -1) { // only care about paths that contain a slash, otherwise this is in the root folder
                        var folderPath = fileName.substring(0, lastSlash);
                        if (fs.getDirectory(folderPath) == null) {
                            vm.error(LuaObject.of("Parent folder '%s' of '%s' does not exist".formatted(folderPath, fileName)));
                            return null;
                        }
                    }
                }
            }
            return LuaObject.of(new LuaFsFileUD(fs.getOrCreateFile(fileName)));
        }));
        reg.register("list", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            var files = fs.getFilesInDirectory(path.getString());
            var dirs = fs.getDirectoriesInDirectory(path.getString());
            return LuaObject.tableFromArray(Stream.concat(files.stream(), dirs.stream().map(x -> x + "/")).map(LuaObject::of).toArray(LuaObject[]::new));
        }));
        reg.register("fileExists", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            return LuaObject.of(fs.getFile(path.getString()) != null);
        }));
        reg.register("directoryExists", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            return LuaObject.of(fs.getDirectory(path.getString()) != null);
        }));
        reg.register("makeDirectory", AtomicLuaFunction.forOneResult(reg, (vm, disk, path) -> {
            var fs = getFsFromDisk(disk);
            fs.createDirectoryAndParents(path.asString());
            return LuaObject.nil();
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
            if (!fs.tryDeleteFile(pathA.asString())) {
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
