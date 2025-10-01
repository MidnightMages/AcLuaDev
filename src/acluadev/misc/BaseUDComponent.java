package acluadev.misc;

import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.api.userdata.LuaUserData;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseUDComponent implements LuaUserData {
    protected abstract String getComponentType();

    private final static AtomicInteger nextComponentUid = new AtomicInteger(0);
    protected int componentUid =  nextComponentUid.getAndIncrement();

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty type = LuaProperty.ofString(this::getComponentType, null);

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty uid = LuaProperty.ofInt(() -> componentUid, null);
}
