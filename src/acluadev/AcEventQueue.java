package acluadev;

import acluadev.misc.BaseUDComponent;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.runtime.utils.UDTranslators;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public class AcEventQueue {
    private final ConcurrentLinkedQueue<LuaObject[]> backing = new ConcurrentLinkedQueue<>();

    public LuaObject[] getQueuedEventOrNull() {
        return backing.poll();
    }

    private void addRaw(String eventName, LuaObject... args) {
        backing.add(Stream.concat(Stream.of(LuaObject.of(eventName)), Arrays.stream(args)).toArray(LuaObject[]::new));
    }

    public void addKeyPressed(KeyEvent keyEvent) {
        addRaw("keyPressed", LuaObject.of(keyEvent.getExtendedKeyCode()));
    }

    public void addKeyReleased(KeyEvent keyEvent) {
        addRaw("keyReleased", LuaObject.of(keyEvent.getExtendedKeyCode()));
    }

    public void addKeyTyped(KeyEvent keyEvent) {
        addRaw("keyTyped", LuaObject.of(Character.toString(keyEvent.getKeyChar())));
    }

    public void addRequestShutdown() {
        addRaw("shutdown");
    }

    public void addComponentAdded(LuaObject comp) {
        assert !comp.isNil();
        var ud = UDTranslators.lo2ud(BaseUDComponent.class, comp);
        assert ud != null;
        addRaw("componentAdded", ud.componentType.get(), comp);
    }
}
