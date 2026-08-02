package acluadev.fs;

import acluadev.misc.RuntimeAssert;
import acluadev.util.list.internal.CharacterList;
import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class LuaFsFileUD implements LuaUserData {
    private final ManagedMassStorageUD parentFilesystemUD;
    private final boolean canRead;
    private final boolean canWrite;
    private final boolean isAppend;
    private final String fsFilePath;
    private int ptr;

    private final boolean autocreate;
    private LuaObject luaIdentity;
    private CharacterList contents;

    public LuaFsFileUD(ManagedMassStorageUD parentFilesystemUD, boolean autocreate, String fsFilePath, boolean canRead,
                       boolean canWrite, boolean isAppend, boolean clearFileOnOpen) {
        RuntimeAssert.RuntimeAssert(canRead || canWrite, "somehow the handle was nonread and nonwrite");
        RuntimeAssert.RuntimeAssert(!isAppend || canWrite, "somehow append was nonwrite");
        RuntimeAssert.RuntimeAssert(!isAppend || !canRead, "somehow append was readable");

        this.autocreate = autocreate;
        this.parentFilesystemUD = parentFilesystemUD;
        this.fsFilePath = fsFilePath;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.isAppend = isAppend;

        finishInit(clearFileOnOpen);
    }

    private void finishInit(boolean clearFileOnOpen) {
        var realDiskFilePath = parentFilesystemUD.getRealFsPath(fsFilePath);
        var exists = parentFilesystemUD.fsProxy.isRegularFile(realDiskFilePath);
        if (!exists) {
            if (autocreate) { // simply act as if we had read the contents
                contents = new CharacterList();
            } else {
                throw new IllegalStateException("somehow a file in the filesystem was missing even though it should be there");
            }
        } else {
            try {
                contents = new CharacterList();
                if (!clearFileOnOpen) {
                    contents.addAllChars(parentFilesystemUD.fsProxy.readString(realDiskFilePath).toCharArray());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ptr = isAppend ? contents.size() : 0; // in the deserialization-case, the ptr value will be overwritten later
        parentFilesystemUD.onFileHandleOpened(fsFilePath, this);

        // if we concluded the filehandle is good, create the actual file on disk so that it shows up in list()
        if (!exists) {
            try {
                parentFilesystemUD.fsProxy.createFile(realDiskFilePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @LuaCallable
    public LuaObject read(int count) {
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        if (!canRead) throw new LuaJavaError("filehandle is writeonly!");
        if (count == -1)
            count = Integer.MAX_VALUE;
        else if (count < -1) {
            throw new LuaJavaError("read length %d is invalid. must be >= -1".formatted(count));
        }

        if (ptr >= contents.size())
            return LuaObject.NIL;

        var sb = new StringBuilder(Math.min(contents.size() - ptr, count));
        for (long i = ptr; i < Math.min(contents.size(), (long) ptr + (long) count); i++) { // needs to be long so that ptr+int32.maxVal doesnt overflow
            sb.append(contents.get((int) i));
            ptr++;
        }

        return LuaObject.of(sb.toString());
    }

    @LuaCallable
    public void write(String s) { // make sure this operation fails if we dont have enough space
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        if (!canWrite) throw new LuaJavaError("filehandle is readonly!");

        for (int i = 0; i < s.length(); i++) {
            if (i < contents.size()) // index already exists -> overwrite
                contents.set(ptr, s.charAt(i));
            else // or just add it
                contents.add(s.charAt(i));

            ptr++;
        }
    }

    @LuaCallable
    public int seek(String relativeTo, int offset) {
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        if (isAppend) throw new LuaJavaError("filehandle can only append!");

        var oldPtr = ptr;
        int newPtr;
        switch (relativeTo) {
            case "start" -> newPtr = offset;
            case "end" -> newPtr = contents.size() - offset;
            case "relative" -> newPtr = ptr + offset;
            default -> throw new LuaJavaError("invalid seek mode '%s' given (argument #1).".formatted(relativeTo));
        }

        if (newPtr < 0 || newPtr > contents.size())
            throw new LuaJavaError("resulting seek position is out of bounds (%d)!".formatted(newPtr));

        ptr = newPtr;
        return oldPtr;
    }

    /**
     * truncates the file to a length of zero
     */
    public void clear() {
        if (contents == null) throw new IllegalStateException("filehandle is already closed");
        contents.clear();
    }

    @LuaCallable
    public void flush() {
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        if (!canWrite)
            throw new LuaJavaError("cannot call flush on a readonly handle");

        try {
            var realPath = parentFilesystemUD.getRealFsPath(fsFilePath);
            var newContents = new String(contents.toCharArray());
            parentFilesystemUD.fsProxy.writeString(realPath, newContents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @LuaCallable
    public void close() {
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        parentFilesystemUD.onFileHandleClosed(fsFilePath, this);
        if (canWrite)
            flush();
        contents = null;
    }

    public void closeForDeletion() { // a simpler version of 'close' which doesnt even save; this is for deletion only
        RuntimeAssert.RuntimeAssert(contents != null, "somehow handle was already closed?");
        contents = null;
        parentFilesystemUD.onFileHandleClosed(fsFilePath, this);
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        throw new IllegalStateException("not implemented");
    }

    @LuaDeserializer
    public static LuaFsFileUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public LuaObject getSelfAsLuaObject() {
        return luaIdentity;
    }

    @Override
    public void setSelfAsLuaObject(LuaObject self) {
        this.luaIdentity = self;
    }

    public long getUnflushedSize() {
        return fsFilePath.length();
    }
}