package acluadev.fs;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.List;
import java.util.Map;

public class LuaFsFile implements LuaUserData {
    final VirtualFile f;

    public LuaFsFile(VirtualFile f) {
        this.f = f;
    }

    @LuaCallable
    public String read() {
        return f.readAllText();
    }

    @LuaCallable
    public void write(String s) {
        f.writeAllText(s);
    }

    @LuaCallable
    public void append(String s) {
        f.appendAllText(s);
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static LuaFsFile todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}