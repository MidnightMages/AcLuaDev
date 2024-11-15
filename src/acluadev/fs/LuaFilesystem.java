package acluadev.fs;

import dev.asdf00.jluavm.runtime.types.AtomicLuaFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;

public class LuaFilesystem {
    private final SandboxedFs fs;

    public LuaFilesystem(SandboxedFs fs) {
        this.fs = fs;
    }

    class LuaFsFile {

        static LuaObject createAsTable(VirtualFile f){
            var rv = LuaObject.table();
            rv.add("read", LuaObject.of(AtomicLuaFunction.vaForManyResults()))
            return rv;
        }
    }

    // https://www.lua.org/manual/5.4/manual.html#6.8
    public LuaObject getTable(){
        var rv = LuaObject.table();
        // TODO return a userdata filehandle table
        rv.set("open", LuaObject.of(AtomicLuaFunction.forOneResult((vm, filename) -> null)));
        return rv;
    }
}
