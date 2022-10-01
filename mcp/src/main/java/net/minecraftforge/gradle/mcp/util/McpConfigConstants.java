package net.minecraftforge.gradle.mcp.util;

public final class McpConfigConstants {

    private McpConfigConstants() {
        throw new IllegalStateException("Can not instantiate an instance of: McpConfigConstants. This is a utility class");
    }

    public static class Data {
        public static final String MAPPINGS = "mappings";
    }

    public static final class Steps {

        public static final class Outputs {
            public static final String JOINED_RAW = "merge";
            public static final String CLIENT_RAW = "rename";
            public static final String SERVER_RAW = "rename";
        }
    }
}
