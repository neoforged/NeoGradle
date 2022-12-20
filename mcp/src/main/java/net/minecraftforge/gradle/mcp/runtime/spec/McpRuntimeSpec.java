package net.minecraftforge.gradle.mcp.runtime.spec;

import com.google.common.collect.Multimap;
import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.common.runtime.spec.TaskTreeAdapter;
import net.minecraftforge.gradle.dsl.common.util.ArtifactSide;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

/**
 * Defines a specification for a MCP runtime.
 */
public class McpRuntimeSpec extends CommonRuntimeSpec {
    private static final long serialVersionUID = -3537760562547500214L;
    private final String mcpVersion;
    private final FileCollection additionalRecompileDependencies;

    /**
     * @param project          The project to use for creating the specification.
     * @param configureProject The project to use for configuring the specification.
     * @param name             The name of the specification.
     * @param mcpVersion       The MCP version to use.
     * @param side             The side to use.
     */
    public McpRuntimeSpec(Project project, Project configureProject, String name, String mcpVersion, ArtifactSide side, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters, FileCollection additionalRecompileDependencies) {
        super(project, configureProject, name, side, preTaskTypeAdapters, postTypeAdapters);
        this.mcpVersion = mcpVersion;
        this.additionalRecompileDependencies = additionalRecompileDependencies;
    }

    /**
     * Extracts the minecraft version from the specified mcp version.
     *
     * @return The minecraft version.
     */
    public String minecraftVersion() {
        return mcpVersion().split("-")[0];
    }

    public String mcpVersion() {
        return mcpVersion;
    }

    public FileCollection additionalRecompileDependencies() {
        return additionalRecompileDependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof McpRuntimeSpec)) return false;
        if (!super.equals(o)) return false;

        McpRuntimeSpec spec = (McpRuntimeSpec) o;

        if (!mcpVersion.equals(spec.mcpVersion)) return false;
        return additionalRecompileDependencies.equals(spec.additionalRecompileDependencies);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + mcpVersion.hashCode();
        result = 31 * result + additionalRecompileDependencies.hashCode();
        return result;
    }
}
