package acluadev.misc;

import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ScreenUD extends BaseAcComponent {
    public ScreenBlockEntity blockEntity;

    public ScreenUD(ScreenBlockEntity x) {
        super("screen");
        blockEntity = x;
    }

    public void drawTextBuffer(TextBufferUD buf) {
        blockEntity.drawTextBuffer(buf);
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return null;
    }

    @LuaDeserializer
    public static ScreenUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return null;
    }
}
