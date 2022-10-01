package net.minecraftforge.gradle.mcp.runtime.spec;

import org.gradle.api.Project;

/**
 * Defines a specification for a MCP runtime.
 *
 * @param project The project to use for creating the specification.
 * @param configureProject The project to use for configuring the specification.
 * @param name The name of the specification.
 * @param mcpVersion The MCP version to use.
 * @param side The side to use.
 * @param preDecompileTaskTreeModifier A task tree modifier to apply to the task tree before decompilation.
 */
public record McpRuntimeSpec(Project project, Project configureProject, String name, String mcpVersion, String side, TaskTreeAdapter preDecompileTaskTreeModifier) {
    /**
     * Extracts the minecraft version from the specified mcp version.
     *
     * @return The minecraft version.
     */
    public String minecraftVersion() {
        return mcpVersion().split("-")[0];
    }
}
