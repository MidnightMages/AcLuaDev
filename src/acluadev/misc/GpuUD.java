package acluadev.misc;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class GpuUD extends BaseAcComponent {
    public final SetBiMap<ScreenBlockEntity, TextBufferUD> screenBufferMap;
    private final HashSet<TextBufferUD> allocatedBuffers;

    @LuaExposed(LuaExposed.Policy.READ)
    public volatile int remainingVideoRam = 110 * 44 * 4; // TODO figure out a proper size
    private final Object remainingVideoRamLockObj = new Object();

    public GpuUD() {
        super("gpu");
        screenBufferMap = new SetBiMap<>();
        allocatedBuffers = new HashSet<>();
    }

    @LuaCallable
    public TextBufferUD newBuffer(int width, int height) {
        if (width <= 0 || height <= 0)
            throw new LuaJavaError("Width and height must be > 0 but were %s and %s respectively.".formatted(width, height));

        var vramNeeded = width * height;
        boolean haveEnoughSpace;
        synchronized (remainingVideoRamLockObj) {
            haveEnoughSpace = remainingVideoRam >= vramNeeded;
            if (haveEnoughSpace) {
                remainingVideoRam -= vramNeeded;
            }
        }
        if (haveEnoughSpace) {
            var buf = new TextBufferUD(width, height, this);
            allocatedBuffers.add(buf); // track it for freeAllBuffers
            return buf;
        }

        throw new LuaJavaError("Not enough video ram remaining to allocate buffer of size (%s,%s)".formatted(width, height));
    }

    void onBufferFreed(TextBufferUD bufferToFree) {
        synchronized (remainingVideoRamLockObj) {
            if (!bufferToFree.isAlive)
                throw new LuaJavaError("Buffer was freed already");
            remainingVideoRam += bufferToFree.width * bufferToFree.height;
            RuntimeAssert.RuntimeAssert(allocatedBuffers.remove(bufferToFree), "tried to free an already freed buffer??");
        }
    }

    @LuaCallable
    public void assignBuffer(TextBufferUD buf, ScreenUD screenUD) {
        ScreenBlockEntity sbe = screenUD.blockEntity;
        if (sbe == null)
            throw new IllegalStateException("internal error trying to find screen");

        screenBufferMap.put(sbe, buf);
        acVm.dirtyBuffer(buf);
    }

    @LuaCallable
    public int freeAllBuffers() {
        int freedBufferCount = 0;
        for (var buf : allocatedBuffers.toArray(TextBufferUD[]::new)) {
            buf.free();
            freedBufferCount++;
        }
        return freedBufferCount;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return null;
    }

    @LuaDeserializer
    public static GpuUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return null;
    }
}
