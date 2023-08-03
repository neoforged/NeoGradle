package net.neoforged.gradle.neoform.util;

public final class NeoFormRuntimeConstants {

    private NeoFormRuntimeConstants() {
        throw new IllegalStateException("Can not instantiate an instance of: NeoFormRuntimeConstants. This is a utility class");
    }

    public static final class Naming {
        public static final class Version {
            public static final String MCP_RUNTIME = "neoFormVersion";
        }
    }
}
