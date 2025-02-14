package acluadev.fs;

import dev.asdf00.jluavm.runtime.types.AtomicLuaFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;

public class LuaFilesystem {
    private final SandboxedFs fs;

    public LuaFilesystem(SandboxedFs fs) {
        this.fs = fs;
    }

    // https://www.lua.org/manual/5.4/manual.html#6.8
    public LuaObject getTable() {
        var rv = LuaObject.table();
        // TODO return a userdata filehandle table
        rv.set("open", AtomicLuaFunction.forOneResult((vm, filename) -> {
            if (!fs.fileExists(filename.asString())) {
                vm.error(LuaObject.of("File '%s' does not exist".formatted(filename.asString())));
                return null;
            }
            return LuaFsFile.createAsUserdata(fs.getFile(filename.asString()));
        }).obj());
        rv.set("list", AtomicLuaFunction.forOneResult((vm, path) ->
                LuaObject.table(fs.getFilesInDirectory(path.getString()).stream().map(LuaFsFile::createAsUserdata).toArray(LuaObject[]::new))).obj());
        rv.set("fileExists", AtomicLuaFunction.forOneResult((vm, path) ->
                LuaObject.of(fs.fileExists(path.getString()))).obj());
        return rv;
    }
}
