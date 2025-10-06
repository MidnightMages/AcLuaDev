package acluadev.misc;

import acluadev.AcEventQueue;
import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
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

    @LuaCallable
    public LuaObject[] list() { // TODO replace with something that can be serialized
        var rets = allComponents.stream().map(LuaComponent::asLuaObj).toArray(LuaObject[][]::new);
        return new LuaObject[]{
                AtomicLuaFunction.forManyResults(null, (vm, state) -> {
                    var oldIdx = state.get(LuaObject.of(0));
                    if (!oldIdx.isLong()) {
                        vm.error(LuaObject.of("Internal error, or someone messed with the iterator state"));
                        return null;
                    }
                    int nuIdx = (int) oldIdx.asLong() + 1;
                    if (nuIdx < rets.length && nuIdx >= 0) {
                        state.set(LuaObject.of(0), LuaObject.of(nuIdx));
                        return rets[nuIdx];
                    } else {
                        return new LuaObject[0];
                    }
                }).obj(),
                LuaObject.table(LuaObject.of(0), LuaObject.of(-1))
        };
    }

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
