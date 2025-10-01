package acluadev.misc;

import dev.asdf00.jluavm.api.userdata.*;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.List;
import java.util.Map;

import static acluadev.Main.eventQueue;

public class ComputerUD extends BaseUDComponent {
    @Override
    protected String getComponentType() {
        return "computer";
    }

    private final boolean enableBeep;


    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty id = LuaProperty.ofString(
            () -> "computer",
            null
    );

    public ComputerUD(boolean enableComputerBeep) {
        enableBeep = enableComputerBeep;
    }

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject nvram = LuaObject.of(new NvramUD());

    @LuaCallable
    public void beep(double freq, double duration) {
        var dur = Math.min(Math.max(duration, 0), 5);
        if (freq < 20 || freq > 2000) {
            throw new LuaJavaError("Invalid frequency %s. Must be in range [20, 2000]".formatted(freq));
        }

        if (enableBeep)
            playBeep(freq, dur);
    }

    @LuaCallable
    public LuaObject[] getMachineEvent() {
        var e = eventQueue.getQueuedEventOrNull();
        return e == null ? new LuaObject[]{LuaObject.NIL} : e;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static ComputerUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }

    private static void playBeep(double frequency, double duration) {
        final float sampleRate = 44100.0f;
        final float volume = 0.05f;
        try {
            AudioFormat audioFormat = new AudioFormat(sampleRate, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat);
            line.open(audioFormat);
            line.start();

            int numSamples = (int) (sampleRate * duration);
            byte[] buffer = new byte[numSamples];

            for (int i = 0; i < numSamples; i++) {
                double t = i / sampleRate;

                // make a square sound
                byte value = (byte) (volume * Byte.MAX_VALUE * (Math.sin(2 * Math.PI * frequency * t) > 0 ? 1 : 0));
                if (i > 1) // smooth over the last few samples to make the lower frequences better to
                    value = (byte) ((value + buffer[i - 1] + buffer[i - 2]) / 3f);

                buffer[i] = value;
            }

            line.write(buffer, 0, buffer.length);
            line.drain();
            line.close();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }
}
