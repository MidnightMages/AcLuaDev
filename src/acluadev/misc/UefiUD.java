package acluadev.misc;

import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public class UefiUD extends BaseAcComponent {
    @SuppressWarnings("FieldCanBeLocal")
    private final int MAX_UEFI_LENGTH = 4096;

    @LuaExposed(LuaExposed.Policy.READWRITE)
    public String data = "";

    public UefiUD(String initialContent) {
        super("uefi");
        data = initialContent;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return null;
    }

    @LuaDeserializer
    public static UefiUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return null;
    }
}