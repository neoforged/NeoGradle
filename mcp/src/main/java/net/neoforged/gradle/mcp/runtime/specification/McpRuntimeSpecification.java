package net.neoforged.gradle.mcp.runtime.specification;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.mcp.runtime.specification.McpSpecification;
import net.neoforged.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;

/**
 * Defines a specification for an MCP runtime.
 */
public class McpRuntimeSpecification extends CommonRuntimeSpecification implements McpSpecification {
    private static final long serialVersionUID = -3537760562547500214L;
    private final String mcpVersion;
    private final FileCollection additionalRecompileDependencies;

    public McpRuntimeSpecification(Project project, String name, String mcpVersion, DistributionType side, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters, FileCollection additionalRecompileDependencies) {
        super(project, name, side, preTaskTypeAdapters, postTypeAdapters);
        this.mcpVersion = mcpVersion;
        this.additionalRecompileDependencies = additionalRecompileDependencies;
    }

    public String getMinecraftVersion() {
        return getMcpVersion().split("-")[0];
    }

    @Override
    public String getMcpVersion() {
        return mcpVersion;
    }

    @Override
    public FileCollection getAdditionalRecompileDependencies() {
        return additionalRecompileDependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof McpRuntimeSpecification)) return false;
        if (!super.equals(o)) return false;

        McpRuntimeSpecification spec = (McpRuntimeSpecification) o;

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

    public static final class Builder extends CommonRuntimeSpecification.Builder<McpRuntimeSpecification, Builder> implements McpSpecification.Builder<McpRuntimeSpecification, Builder> {

        private Provider<String> mcpVersion;
        private boolean hasConfiguredMcpVersion = false;
        private FileCollection additionalDependencies;

        private Builder(Project project) {
            super(project);
            this.additionalDependencies = project.getObjects().fileCollection();
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public static Builder from(final Project project) {
            return new Builder(project);
        }

        protected void configureBuilder() {
            super.configureBuilder();
            final McpRuntimeExtension runtimeExtension = this.getProject().getExtensions().getByType(McpRuntimeExtension.class);

            if (!this.hasConfiguredMcpVersion) {
                this.mcpVersion = runtimeExtension.getDefaultVersion();
            }
        }

        @Override
        public Builder withMcpVersion(final Provider<String> mcpVersion) {
            this.mcpVersion = mcpVersion;
            this.hasConfiguredMcpVersion = true;
            return getThis();
        }

        @Override
        public Builder withMcpVersion(final String mcpVersion) {
            if (mcpVersion == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();

            return withMcpVersion(project.provider(() -> mcpVersion));
        }

        @Override
        public Builder withAdditionalDependencies(final FileCollection files) {
            this.additionalDependencies = this.additionalDependencies.plus(files);
            return getThis();
        }

        public McpRuntimeSpecification build() {
            return new McpRuntimeSpecification(project, namePrefix, mcpVersion.get(), distributionType.get(), preTaskAdapters, postTaskAdapters, additionalDependencies);
        }
    }
}
