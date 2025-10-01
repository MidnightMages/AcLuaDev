package acluadev.misc;

import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.api.userdata.LuaUserData;

public abstract class BaseUDComponent implements LuaUserData {
    protected abstract String getComponentType();

    private static int nextComponentUid = 0;
    protected int componentUid = nextComponentUid++;

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty type = LuaProperty.ofString(this::getComponentType, null);

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty uid = LuaProperty.ofInt(() -> componentUid, null);
}
