package net.minecraftforge.gradle.mcp.runtime.spec.builder;

import net.minecraftforge.gradle.dsl.common.runtime.spec.builder.CommonRuntimeSpecBuilder;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;

public final class McpRuntimeSpecBuilder extends CommonRuntimeSpecBuilder<McpRuntimeSpec, McpRuntimeSpecBuilder> {

    private Provider<String> mcpVersion;
    private boolean hasConfiguredMcpVersion = false;
    private FileCollection additionalDependencies;

    private McpRuntimeSpecBuilder(Project project) {
        super(project);
        this.additionalDependencies = project.getObjects().fileCollection();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected McpRuntimeSpecBuilder getThis() {
        return this;
    }

    public static McpRuntimeSpecBuilder from(final Project project) {
        return new McpRuntimeSpecBuilder(project);
    }

    protected void configureBuilder() {
        super.configureBuilder();
        final McpRuntimeExtension runtimeExtension = this.configureProject.getExtensions().getByType(McpRuntimeExtension.class);

        if (!this.hasConfiguredMcpVersion) {
            this.mcpVersion = runtimeExtension.getDefaultVersion();
        }
    }

    public McpRuntimeSpecBuilder withMcpVersion(final Provider<String> mcpVersion) {
        this.mcpVersion = mcpVersion;
        this.hasConfiguredMcpVersion = true;
        return getThis();
    }

    public McpRuntimeSpecBuilder withMcpVersion(final String mcpVersion) {
        if (mcpVersion == null) // Additional null check for convenient loading of versions from dependencies.
            return getThis();

        return withMcpVersion(project.provider(() -> mcpVersion));
    }

    public McpRuntimeSpecBuilder withAdditionalDependencies(final FileCollection files) {
        this.additionalDependencies = this.additionalDependencies.plus(files);
        return getThis();
    }

    public McpRuntimeSpec build() {
        return new McpRuntimeSpec(project, configureProject, namePrefix, mcpVersion.get(), side.get(), preTaskAdapters, postTaskAdapters, additionalDependencies);
    }
}
