package acluadev.misc;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ScreenUD extends BaseUDComponent {

    private final BiConsumer<LuaObject[], Boolean> onPrint;

    public ScreenUD(BiConsumer<LuaObject[], Boolean> onPrint) {
        this.onPrint = onPrint;
    }

    @Override
    protected String getComponentType() {
        return "screen";
    }

    // TODO implement lua api
    @LuaCallable
    public void printInline(LuaObject[] args) {
        onPrint.accept(args, false);
    }

    @LuaCallable
    public void print(LuaObject[] args) {
        onPrint.accept(args, true);
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        return new byte[0];
    }

    @LuaDeserializer
    public static ScreenUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}
