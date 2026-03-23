package acluadev;

public record Config(
        String luaRootDirectory,
        boolean allowPhysicalFilesystemWrites,
        boolean enableComputerBeep,
        int screenCount
){};
