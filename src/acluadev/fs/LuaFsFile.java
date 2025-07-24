package acluadev.fs;

import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.internals.LuaVM_RT;
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

    private void write(LuaVM_RT vm, LuaObject x) {
        f.writeAllText(x.asString());
    }

    private void append(LuaVM_RT vm, LuaObject x) {
        f.appendAllText(x.asString());
    }

    static LuaObject createAsUserdata(VirtualFile f) {
        var inst = new LuaFsFile(f);
        //var rv = LuaObject.of(inst); // TODO wait for userdata support
        var rv = LuaObject.table();
        rv.set("read", AtomicLuaFunction.vaForManyResults(null, inst::read).obj());
        rv.set("write", AtomicLuaFunction.forZeroResults(null, inst::write).obj());
        rv.set("append", AtomicLuaFunction.forZeroResults(null, inst::append).obj());
        return rv;
    }
}