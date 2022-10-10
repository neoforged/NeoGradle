package net.minecraftforge.gradle.mcp.runtime.spec.builder;

import net.minecraftforge.gradle.common.util.ArtifactSide;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.spec.TaskTreeAdapter;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

public final class McpRuntimeSpecBuilder {

    private final Project project;
    private Project configureProject;
    private String namePrefix = "";
    private Provider<String> mcpVersion;
    private boolean hasConfiguredMcpVersion = false;
    private Provider<ArtifactSide> side;
    private boolean hasConfiguredSide = false;

    private TaskTreeAdapter preDecompileTaskTreeModifier = null;

    private McpRuntimeSpecBuilder(Project project) {
        this.project = project;
        this.configureProject = project;
    }

    public static McpRuntimeSpecBuilder from(final Project project) {
        final McpRuntimeSpecBuilder builder =  new McpRuntimeSpecBuilder(project);

        configureBuilder(builder);

        return builder;
    }

    private static void configureBuilder(McpRuntimeSpecBuilder builder) {
        final McpRuntimeExtension runtimeExtension = builder.configureProject.getExtensions().getByType(McpRuntimeExtension.class);

        if (!builder.hasConfiguredSide) {
            builder.side = runtimeExtension.getDefaultSide();
        }
        if (!builder.hasConfiguredMcpVersion) {
            builder.mcpVersion = runtimeExtension.getDefaultVersion();
        }
    }

    public McpRuntimeSpecBuilder withName(final String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    public McpRuntimeSpecBuilder withMcpVersion(final Provider<String> mcpVersion) {
        this.mcpVersion = mcpVersion;
        this.hasConfiguredMcpVersion = true;
        return this;
    }

    public McpRuntimeSpecBuilder withMcpVersion(final String mcpVersion) {
        if (mcpVersion == null) // Additional null check for convenient loading of versions from dependencies.
            return this;

        return withMcpVersion(project.provider(() -> mcpVersion));
    }

    public McpRuntimeSpecBuilder withSide(final Provider<ArtifactSide> side) {
        this.side = side;
        this.hasConfiguredSide = true;
        return this;
    }

    public McpRuntimeSpecBuilder withSide(final ArtifactSide side) {
        if (side == null) // Additional null check for convenient loading of sides from dependencies.
            return this;

        return withSide(project.provider(() -> side));
    }

    public McpRuntimeSpecBuilder withPreDecompileTaskTreeModifier(TaskTreeAdapter preDecompileTaskTreeModifier) {
        if (this.preDecompileTaskTreeModifier == null) {
            this.preDecompileTaskTreeModifier = preDecompileTaskTreeModifier;
            return this;
        }

        this.preDecompileTaskTreeModifier = this.preDecompileTaskTreeModifier.andThen(preDecompileTaskTreeModifier);
        return this;
    }

    public McpRuntimeSpecBuilder configureFromProject(Project configureProject) {
        this.configureProject = configureProject;

        configureBuilder(this);

        return this;
    }

    public McpRuntimeSpec build() {
        return new McpRuntimeSpec(project, configureProject, namePrefix, mcpVersion.get(), side.get(), preDecompileTaskTreeModifier);
    }
}
