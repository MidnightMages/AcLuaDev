package acluadev.misc;

import acluadev.LuaVirtualMachine;
import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

public final class ComputerUD extends BaseAcComponent {
    private final ConcurrentLinkedQueue<LuaObject[]> eventQueue = new ConcurrentLinkedQueue<>();
    private final long bootTimeMillis = System.currentTimeMillis();

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject uefi;

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject nvram = LuaObject.nil(); // requires mainboard tier 2 or higher

    @LuaExposed(LuaExposed.Policy.READ)
    public LuaObject tpm = LuaObject.nil(); // requires mainboard tier 3

    public ComputerUD(UefiUD uefi, NvramUD nvram) {
        super("computer");
        this.uefi = LuaObject.of(uefi);
        this.nvram = LuaObject.of(nvram);
    }

    /**
     * This method may be called from outside the LUA thread and enqueues a custom machine event to be read by the host
     * LUA program.
     */
    public void triggerMachineEvent(String eventName, LuaObject... args) {
        eventQueue.add(Stream.concat(Stream.of(LuaObject.of(eventName)), Arrays.stream(args)).toArray(LuaObject[]::new));
    }

    @LuaCallable
    public void beep(double freq, double duration) {
        var dur = Math.min(Math.max(duration, 0), 5);
        if (freq < 20 || freq > 2000) {
            throw new LuaJavaError("Invalid frequency %s. Must be in range [20, 2000]".formatted(freq));
        }

        if (acVm.enableBeep)
            playBeep(freq, dur);
    }
    @LuaCallable
    public LuaObject[] getMachineEvent() {
        LuaObject[] e = eventQueue.poll();
        return e == null ? new LuaObject[]{LuaObject.NIL} : e;
    }

    @LuaCallable
    public double getEpoch() {
        return System.currentTimeMillis() / 1000d;
    }

    @LuaCallable
    public long getEpochMs() {
        return System.currentTimeMillis();
    }

    @LuaCallable
    public String getDate() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX").format(new Date());
    }

    @LuaCallable
    public double getUptime() {
        return (System.currentTimeMillis() - bootTimeMillis) / 1000d;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return null;
    }

    @LuaDeserializer
    public static ComputerUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
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
