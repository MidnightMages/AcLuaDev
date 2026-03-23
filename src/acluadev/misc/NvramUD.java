package acluadev.misc;

import acluadev.LuaVirtualMachine;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public final class NvramUD extends BaseAcComponent {
    public NvramUD() {
        super("nvram");
    }

    private NvramUD(LuaVirtualMachine acVm) {
        super("nvram", acVm, true);
    }

    private final HashMap<String, LuaObject> backing = new HashMap<>();

    @Override
    public LuaObject luaGeneralGet(LuaObject key) throws LuaJavaError {
        if (!key.isString())
            return null;

        return backing.getOrDefault(key.asString(), null);
    }

    @Override
    public boolean luaGeneralSet(LuaObject key, LuaObject value) throws LuaJavaError {
        if (!key.isString())
            throw new LuaJavaError("Only string keys are supported. Not keys of type %s!".formatted(key.getTypeAsString()));
        if (!value.isType(LuaObject.Types.NUMBER | LuaObject.Types.STRING | LuaObject.Types.NIL | LuaObject.Types.BOOLEAN))
            throw new LuaJavaError("Only primitive, immutable values are supported (number, string, nil, bool). Not values of type %s!".formatted(value.getTypeAsString()));

        if (value.isNil())
            backing.remove(key.asString());
        else
            backing.put(key.asString(), value);
        return true;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return null;
    }

    @LuaDeserializer
    public static NvramUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return null;
    }
}