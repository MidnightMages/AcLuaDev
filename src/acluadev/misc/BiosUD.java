package acluadev.misc;

import dev.asdf00.jluavm.api.userdata.*;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.List;
import java.util.Map;

public class BiosUD implements LuaUserData {

    private String biosFile = "";

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty id = LuaProperty.ofString(
            () -> "bios",
            null
    );

    @LuaCallable
    public String getData() {
        return biosFile;
    }

    @LuaCallable
    public void setData(String s) {
        biosFile = s;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static BiosUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}
