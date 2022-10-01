package net.minecraftforge.gradle.mcp.util;

import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class McpRuntimeUtils {

    private McpRuntimeUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: McpRuntimeUtils. This is a utility class");
    }

    public static String buildTaskName(final McpRuntimeSpec runtimeSpec, final String defaultName) {
        if (runtimeSpec.name().isEmpty())
            return defaultName;

        return runtimeSpec.name() + StringUtils.capitalize(defaultName);
    }

    public static String buildTaskName(final McpRuntimeDefinition runtimeSpec, final String defaultName) {
        return buildTaskName(runtimeSpec.spec(), defaultName);
    }

    public static void withExtractedMcpVersion(@Nullable final String version, final Consumer<String> mcpVersion) {
        if (version == null)
            return;

        final String[] split = version.split("-");
        if (split.length == 1)
            return;

        mcpVersion.accept(split[1]);
    }
}
