package io.github.oldmanpushcart.moss.util.env;

public enum CpuArch {

    X86("x86"),
    X64("amd64", "x86_64"),
    ARM("arm"),
    AARCH64("aarch64"),
    UNKNOWN("unknown");

    public static final CpuArch CURRENT = getCurrent();

    private final String[] names;

    CpuArch(String... names) {
        this.names = names;
    }

    private static CpuArch getCurrent() {
        final var osArch = System.getProperty("os.arch").toLowerCase();
        for (final var arch : CpuArch.values()) {
            for (final var name : arch.names) {
                if (osArch.equals(name)) {
                    return arch;
                }
            }
        }
        return UNKNOWN;
    }

}
