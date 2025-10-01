package acluadev.misc;

import acluadev.AcEventQueue;
import dev.asdf00.jluavm.api.userdata.*;
import dev.asdf00.jluavm.runtime.types.LuaJavaApiFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static acluadev.Main.createIterFunction;

public class ComponentRegistryUD implements LuaUserData {
    private final ArrayList<LuaComponent> allComponents = new ArrayList<>();

    public void registerComponent(BaseUDComponent component) {
        allComponents.add(new LuaComponent(component.type.get().asString(), LuaObject.of(component)));
    }

    public record LuaComponent(String type, LuaObject comp) {
        public LuaObject[] asLuaObj() {
            return new LuaObject[]{LuaObject.of(type), comp};
        }
    }

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject list = LuaObject.of(createIterFunction(() -> allComponents.stream().map(LuaComponent::asLuaObj).toArray(LuaObject[][]::new)));

    @LuaCallable
    public LuaObject getFirst(String componentType) {
        return allComponents.stream().filter(x -> x.type().equals(componentType)).map(LuaComponent::comp).findFirst().orElse(LuaObject.NIL);
    }

    public void addAllComponentsToEventQueue(AcEventQueue queue) {
        for (var comp : allComponents)
            queue.addComponentAdded(comp.comp());
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static ComponentRegistryUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}
