package net.minecraftforge.gradle.common.util;

public final class NamingConstants {

    private NamingConstants() {
        throw new IllegalStateException("Can not instantiate an instance of: NamingConstants. This is a utility class");
    }

    public static class Version {

        public static final String VERSION = "version";
        public static final String MINECRAFT_VERSION = "minecraft";
    }
}
