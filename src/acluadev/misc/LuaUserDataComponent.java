package acluadev.misc;

import acluadev.LuaVirtualMachine;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
public interface LuaUserDataComponent extends LuaUserData {
    String getComponentType();

    /**
     * Purely for passing data to the userdata object upon **initial** vm creation (NOT during deserialization).
     * You may provide an empty body for this method if you do not require the provided objects.
     * @param acVm A reference to the parent {@link LuaVirtualMachine} object.
     */
    void onVmInit(LuaVirtualMachine acVm);

    /**
     * If this is called, the lua object shall no longer be accessible in any way. This is generally called when the
     * item / block that provides this userdata is removed or destroyed and therefore the component no longer exists.
     */
    void makeObjectInaccessible();
}
