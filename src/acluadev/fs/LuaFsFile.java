package acluadev.fs;

import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.AtomicLuaFunction;
import dev.asdf00.jluavm.runtime.types.ILuaUserData;
import dev.asdf00.jluavm.runtime.types.LuaObject;

public class LuaFsFile implements ILuaUserData {
    final VirtualFile f;

    public LuaFsFile(VirtualFile f) {
        this.f = f;
    }

    private LuaObject[] read(LuaVM_RT vm, LuaObject[] args) {
        return new LuaObject[]{LuaObject.of(f.readAllText())};
    }

    static LuaObject createAsUserdata(VirtualFile f){
        var inst = new LuaFsFile(f);
        var rv = LuaObject.of(inst);
        rv.set("read", LuaObject.of(AtomicLuaFunction.vaForManyResults(inst::read)));
        return rv;
    }
}