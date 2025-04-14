package io.github.oldmanpushcart.moss.util.env;

/**
 * 操作系统类型
 */
public enum OsType {

    /**
     * Windows
     */
    WINDOWS("windows"),

    /**
     * Linux
     */
    LINUX("linux"),

    /**
     * MacOS
     */
    MACOS("macos"),

    /**
     * 未知
     */
    UNKNOWN("unknown");

    public static final OsType CURRENT = getCurrent();

    private final String name;

    OsType(String name) {
        this.name = name;
    }

    private static OsType getCurrent() {
        final var osName = System.getProperty("os.name").toLowerCase().replaceAll("\\s+", "");
        for (final var osType : OsType.values()) {
            if (osName.contains(osType.name)) {
                return osType;
            }
        }
        return UNKNOWN;
    }

}
