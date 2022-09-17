package net.minecraftforge.gradle.mcp.runtime.spec;

import org.gradle.api.Project;

public record McpRuntimeSpec(Project project, String namePrefix, String mcpVersion, String side, TaskTreeAdapter preDecompileTaskTreeModifier) {
}
