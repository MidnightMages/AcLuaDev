package acluadev.misc;

import acluadev.LuaVirtualMachine;
import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ComponentRegistryUD implements LuaUserData {
    private LuaObject luaIdentity;

    private final LuaVirtualMachine lvm;
    private final Object componentModifyLockObj = new Object();
    private final ArrayList<LuaUserDataComponent> allComponents = new ArrayList<>();

    public ComponentRegistryUD(LuaVirtualMachine lvm) {
        this.lvm = lvm;
    }

    @LuaCallable
    public LuaObject list() {
        LuaObject[] rets;
        synchronized (componentModifyLockObj) {
            rets = allComponents.stream()
                    .map(x ->
                            LuaObject.of(LuaObject.of(x.getComponentType()), LuaObject.of(x)) // create ARRAY
                    )
                    .toArray(LuaObject[]::new);
        }

        return LuaObject.of(LuaVirtualMachine.BUILTIN_FUNCTIONS.getFunction("$internal.unpacking_iterator",
                LuaObject.tableFromArray(rets),
                new LuaObject[]{LuaObject.of(1)}
        ));
    }

    @LuaCallable
    public LuaObject getFirst(String componentType) {
        synchronized (componentModifyLockObj) {
            return allComponents.stream()
                    .filter(x -> x.getComponentType().equals(componentType))
                    .map(LuaObject::of)
                    .findFirst()
                    .orElse(LuaObject.NIL);
        }
    }

    Map<String, Integer> massStorageSortOrder = Map.of(
            "hdd", 0,
            "floppy", 1
    );

    public void addComponentInitAndNotify(LuaUserDataComponent component) {
        component.onVmInit(lvm);
        synchronized (componentModifyLockObj) {
            allComponents.add(component);
        }
        lvm.triggerMachineEvent("componentAdded", LuaObject.of(component.getComponentType()), LuaObject.of(component));
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return null;
    }

    @LuaDeserializer
    public static ComponentRegistryUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        return null;
    }

    @Override
    public final LuaObject getSelfAsLuaObject() {
        return luaIdentity;
    }

    @Override
    public final void setSelfAsLuaObject(LuaObject self) {
        luaIdentity = self;
    }
}
