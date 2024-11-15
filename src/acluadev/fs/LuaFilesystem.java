package acluadev.fs;

import dev.asdf00.jluavm.runtime.types.AtomicLuaFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;

public class LuaFilesystem {
    private final SandboxedFs fs;

    public LuaFilesystem(SandboxedFs fs) {
        this.fs = fs;
    }

    // https://www.lua.org/manual/5.4/manual.html#6.8
    public LuaObject getTable(){
        var rv = LuaObject.table();
        // TODO return a userdata filehandle table
        rv.set("open", LuaObject.of(AtomicLuaFunction.forOneResult((vm, filename) -> LuaFsFile.createAsUserdata(fs.getFile(filename.asString())))));
        return rv;
    }
}
