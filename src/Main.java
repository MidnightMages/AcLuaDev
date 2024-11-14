import dev.asdf00.jluavm.internals.LuaVM_RT;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        var vm = LuaVM_RT.create();
    }
}