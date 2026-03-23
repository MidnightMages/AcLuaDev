package acluadev.misc;

import acluadev.Console;
import acluadev.LuaVirtualMachine;
import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.stream.Stream;

public class ScreenBlockEntity {
    private Console console;

    private LuaVirtualMachine vm;

    public void drawTextBuffer(TextBufferUD buf) {
        console.drawTextBuffer(buf);
    }

    public void init(LuaVirtualMachine vm) {
        this.vm = vm;
        console.onKeyPressed = this::addKeyPressed;
        console.onKeyReleased = this::addKeyReleased;
        console.onKeyTyped = this::addKeyTyped;
    }

    public void clear() {
        console.clear();
    }

    public void create() {
        console = Console.createConsole();
    }

    public void addKeyPressed(KeyEvent keyEvent) {
        vm.triggerMachineEvent ("keyPressed", LuaObject.of(keyEvent.getExtendedKeyCode()));
    }

    public void addKeyReleased(KeyEvent keyEvent) {
        vm.triggerMachineEvent ("keyReleased", LuaObject.of(keyEvent.getExtendedKeyCode()));
    }

    public void addKeyTyped(KeyEvent keyEvent) {
        vm.triggerMachineEvent ("keyTyped", LuaObject.of(Character.toString(keyEvent.getKeyChar())));
    }
}
