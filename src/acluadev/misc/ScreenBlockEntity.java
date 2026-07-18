package acluadev.misc;

import acluadev.Console;
import acluadev.LuaVirtualMachine;
import dev.asdf00.jluavm.runtime.types.LuaObject;

import java.awt.event.KeyEvent;

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
        console.onTextPasted = this::addTextPasted;
    }

    public void clear() {
        console.clear();
    }

    public void create() {
        console = Console.createConsole();
    }

    private LuaObject[] packKeyEvent(String stringRepresentation, int keyCode, int scanCode, int modifiers) {
        return new LuaObject[]{LuaObject.of(stringRepresentation), LuaObject.of(keyCode), LuaObject.of(scanCode), LuaObject.of(modifiers)};
    }

    private LuaObject[] translateKeysToMinecraftFormat(KeyEvent keyEvent) {int javaCode = keyEvent.getExtendedKeyCode();

        int mcKeyCode = switch (javaCode) {
            case KeyEvent.VK_ESCAPE -> 256;

            case KeyEvent.VK_TAB -> 258;
            case KeyEvent.VK_ENTER -> 257;
            case KeyEvent.VK_BACK_SPACE -> 259;

            case KeyEvent.VK_INSERT -> 260;
            case KeyEvent.VK_DELETE -> 261;

            case KeyEvent.VK_RIGHT -> 262;
            case KeyEvent.VK_LEFT -> 263;
            case KeyEvent.VK_DOWN -> 264;
            case KeyEvent.VK_UP -> 265;

            case KeyEvent.VK_PAGE_UP -> 266;
            case KeyEvent.VK_PAGE_DOWN -> 267;
            case KeyEvent.VK_HOME -> 268;
            case KeyEvent.VK_END -> 269;

            case KeyEvent.VK_CAPS_LOCK -> 280;
            case KeyEvent.VK_SCROLL_LOCK -> 281;
            case KeyEvent.VK_NUM_LOCK -> 282;
            case KeyEvent.VK_PRINTSCREEN -> 283;
            case KeyEvent.VK_PAUSE -> 284;

            case KeyEvent.VK_F1 -> 290;
            case KeyEvent.VK_F2 -> 291;
            case KeyEvent.VK_F3 -> 292;
            case KeyEvent.VK_F4 -> 293;
            case KeyEvent.VK_F5 -> 294;
            case KeyEvent.VK_F6 -> 295;
            case KeyEvent.VK_F7 -> 296;
            case KeyEvent.VK_F8 -> 297;
            case KeyEvent.VK_F9 -> 298;
            case KeyEvent.VK_F10 -> 299;
            case KeyEvent.VK_F11 -> 300;
            case KeyEvent.VK_F12 -> 301;

            case KeyEvent.VK_NUMPAD0 -> 320;
            case KeyEvent.VK_NUMPAD1 -> 321;
            case KeyEvent.VK_NUMPAD2 -> 322;
            case KeyEvent.VK_NUMPAD3 -> 323;
            case KeyEvent.VK_NUMPAD4 -> 324;
            case KeyEvent.VK_NUMPAD5 -> 325;
            case KeyEvent.VK_NUMPAD6 -> 326;
            case KeyEvent.VK_NUMPAD7 -> 327;
            case KeyEvent.VK_NUMPAD8 -> 328;
            case KeyEvent.VK_NUMPAD9 -> 329;

            case KeyEvent.VK_SHIFT -> 340;
            case KeyEvent.VK_CONTROL -> 341;
            case KeyEvent.VK_ALT -> 342;
            case KeyEvent.VK_WINDOWS, KeyEvent.VK_META -> 343;

            default -> {
                if (javaCode >= KeyEvent.VK_A && javaCode <= KeyEvent.VK_Z) {
                    yield 'A' + (javaCode - KeyEvent.VK_A);
                }
                else if (javaCode >= KeyEvent.VK_0 && javaCode <= KeyEvent.VK_9) {
                    yield '0' + (javaCode - KeyEvent.VK_0);
                }
                else {
                    yield 0;
                }
            }
        };


        int mcScanCode = switch (keyEvent.getExtendedKeyCode()) {
            case KeyEvent.VK_UP -> 328;
            case KeyEvent.VK_DOWN -> 336;
            case KeyEvent.VK_LEFT -> 331;
            case KeyEvent.VK_RIGHT -> 333;

            case KeyEvent.VK_HOME -> 327;
            case KeyEvent.VK_END -> 335;
            case KeyEvent.VK_PAGE_UP -> 329;
            case KeyEvent.VK_PAGE_DOWN -> 337;
            case KeyEvent.VK_INSERT -> 338;
            case KeyEvent.VK_DELETE -> 339;

            default -> keyEvent.getKeyCode() | 256; // fallback
        };

        int modifiers = 0;
        int swingMods = keyEvent.getModifiersEx();
        if ((swingMods & KeyEvent.SHIFT_DOWN_MASK) != 0)            modifiers |= 1;
        if ((swingMods & KeyEvent.CTRL_DOWN_MASK) != 0)            modifiers |= 2;
        if ((swingMods & KeyEvent.ALT_DOWN_MASK) != 0)            modifiers |= 4;


        // fallback for non-printable keys
        var stringRepresentation = switch (keyEvent.getExtendedKeyCode()) {
            case KeyEvent.VK_SPACE -> " ";
            case KeyEvent.VK_TAB -> "\t";
            case KeyEvent.VK_ENTER -> "\n";
            case KeyEvent.VK_BACK_SPACE -> "\b";

            default -> {
                var printedChar = keyEvent.getKeyChar();
                yield printedChar != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(printedChar) ? String.valueOf(printedChar) : "";
            }
        };

        return packKeyEvent(stringRepresentation, mcKeyCode, mcScanCode, modifiers);
    }


    public void addKeyPressed(KeyEvent keyEvent) {
        vm.triggerMachineEvent("keyPressed", translateKeysToMinecraftFormat(keyEvent));
    }

    public void addKeyReleased(KeyEvent keyEvent) {
        vm.triggerMachineEvent("keyReleased", translateKeysToMinecraftFormat(keyEvent));
    }

    public void addKeyTyped(KeyEvent keyEvent) {
        vm.triggerMachineEvent ("keyTyped", LuaObject.of(Character.toString(keyEvent.getKeyChar())));
    }

    public void addTextPasted(String p) {
        vm.triggerMachineEvent("textPasted", LuaObject.of(p));
    }
}
