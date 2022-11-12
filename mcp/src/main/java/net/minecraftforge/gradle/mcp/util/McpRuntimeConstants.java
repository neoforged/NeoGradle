package net.minecraftforge.gradle.mcp.util;

public final class McpRuntimeConstants {

    private McpRuntimeConstants() {
        throw new IllegalStateException("Can not instantiate an instance of: McpRuntimeConstants. This is a utility class");
    }

    public static final class Naming {
        public static final class Version {
            public static final String MCP_RUNTIME = "mcpRuntime";
        }
    }
}
