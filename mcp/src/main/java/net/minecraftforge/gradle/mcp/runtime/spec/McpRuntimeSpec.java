package net.minecraftforge.gradle.mcp.runtime.spec;

import com.google.common.collect.Multimap;
import net.minecraftforge.gradle.common.util.ArtifactSide;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

import java.io.Serializable;
import java.util.Objects;

/**
 * Defines a specification for a MCP runtime.
 */
public final class McpRuntimeSpec implements Serializable {
    private static final long serialVersionUID = -3537760562547500214L;
    private final Project project;
    private final Project configureProject;
    private final String name;
    private final String mcpVersion;
    private final ArtifactSide side;
    private final Multimap<String, TaskTreeAdapter> preTaskTypeAdapters;
    private final Multimap<String, TaskTreeAdapter> postTypeAdapters;
    private final FileCollection additionalRecompileDependencies;

    /**
     * @param project          The project to use for creating the specification.
     * @param configureProject The project to use for configuring the specification.
     * @param name             The name of the specification.
     * @param mcpVersion       The MCP version to use.
     * @param side             The side to use.
     */
    public McpRuntimeSpec(Project project, Project configureProject, String name, String mcpVersion, ArtifactSide side, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters, FileCollection additionalRecompileDependencies) {
        this.project = project;
        this.configureProject = configureProject;
        this.name = name;
        this.mcpVersion = mcpVersion;
        this.side = side;
        this.preTaskTypeAdapters = preTaskTypeAdapters;
        this.postTypeAdapters = postTypeAdapters;
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

    public Project project() {
        return project;
    }

    public Project configureProject() {
        return configureProject;
    }

    public String name() {
        return name;
    }

    public String mcpVersion() {
        return mcpVersion;
    }

    public ArtifactSide side() {
        return side;
    }

    public Multimap<String, TaskTreeAdapter> preTaskTypeAdapters() {
        return preTaskTypeAdapters;
    }

    public Multimap<String, TaskTreeAdapter> postTypeAdapters() {
        return postTypeAdapters;
    }

    public FileCollection additionalRecompileDependencies() {
        return additionalRecompileDependencies;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final McpRuntimeSpec that = (McpRuntimeSpec) obj;
        return Objects.equals(this.project, that.project) &&
                Objects.equals(this.configureProject, that.configureProject) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.mcpVersion, that.mcpVersion) &&
                Objects.equals(this.side, that.side) &&
                Objects.equals(this.preTaskTypeAdapters, that.preTaskTypeAdapters) &&
                Objects.equals(this.postTypeAdapters, that.postTypeAdapters) &&
                Objects.equals(this.additionalRecompileDependencies, that.additionalRecompileDependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, configureProject, name, mcpVersion, side, preTaskTypeAdapters, postTypeAdapters, additionalRecompileDependencies);
    }

    @Override
    public String toString() {
        return "McpRuntimeSpec[" +
                "project=" + project + ", " +
                "configureProject=" + configureProject + ", " +
                "name=" + name + ", " +
                "mcpVersion=" + mcpVersion + ", " +
                "side=" + side + ", " +
                "preTaskTypeAdapters=" + preTaskTypeAdapters + ", " +
                "postTypeAdapters=" + postTypeAdapters + ", " +
                "additionalRecompileDependencies=" + additionalRecompileDependencies + ']';
    }

}
