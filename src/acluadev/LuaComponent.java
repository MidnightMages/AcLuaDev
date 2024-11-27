package acluadev;

import dev.asdf00.jluavm.runtime.types.LuaObject;

public record LuaComponent(String type, LuaObject comp) {
    public LuaObject[] asLuaObj() {
        return new LuaObject[]{LuaObject.of(type), comp};
    }
}
