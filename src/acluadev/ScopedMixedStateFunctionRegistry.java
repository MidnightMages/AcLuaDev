package acluadev;

import dev.asdf00.jluavm.api.functions.MixedStateFunctionRegistry;
import dev.asdf00.jluavm.runtime.types.LuaJavaApiFunction;
import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.util.function.Function;

public class ScopedMixedStateFunctionRegistry extends MixedStateFunctionRegistry {
    public ScopedMixedStateFunctionRegistry(String id) {
        super(id);
    }

    public SubRegistry forTable(String tableName) {
        return new SubRegistry(tableName);
    }

    public class SubRegistry {
        private final String prefix;

        private SubRegistry(String tableName) {
            this.prefix = (tableName == null || tableName.isBlank()) ? "" : tableName + ".";
        }

        public void register(String name, LuaJavaApiFunction apiFunction) {
            ScopedMixedStateFunctionRegistry.this.register(prefix + name, apiFunction);
        }

        public void register(String name, Class<? extends LuaJavaApiFunction> clazz, Function<LuaObject, LuaJavaApiFunction> instantiator) {
            ScopedMixedStateFunctionRegistry.this.register(prefix + name, clazz, instantiator);
        }
    }

    public void addFunctionsToTable(LuaObject env) {
        // add all noninternal functions
        // all these functions are assumed to be stateless
        for (String fName : this.getAllNames()) {
            if (fName.charAt(0) == '$') {
                // internal function, do not add to _G
                continue;
            }
            if (fName.indexOf('.') < 0) {
                // top level
                env.set(fName, LuaObject.of(this.getFunction(fName)));
            } else {
                var path = fName.split("\\.");
                assert path.length == 2 : "only ever expected tbl.funcname, got: " + fName;
                var tbl = env.get(path[0]);
                if (tbl.isNil()) {
                    tbl = LuaObject.table();
                    env.set(path[0], tbl);
                }
                tbl.set(path[1], LuaObject.of(this.getFunction(fName)));
            }
        }
    }
}
