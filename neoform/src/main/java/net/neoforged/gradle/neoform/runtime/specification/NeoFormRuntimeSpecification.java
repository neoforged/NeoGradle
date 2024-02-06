package net.neoforged.gradle.neoform.runtime.specification;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskCustomizer;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV2;
import net.neoforged.gradle.dsl.neoform.runtime.specification.NeoFormSpecification;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.util.FileUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Defines a specification for an MCP runtime.
 */
public class NeoFormRuntimeSpecification extends CommonRuntimeSpecification implements NeoFormSpecification {
    private final File neoFormArchive;
    private final NeoFormConfigConfigurationSpecV2 config;
    private final FileCollection additionalRecompileDependencies;

    private NeoFormRuntimeSpecification(Project project,
                                          String version,
                                          File neoFormArchive,
                                          NeoFormConfigConfigurationSpecV2 config,
                                          DistributionType side,
                                          Multimap<String, TaskTreeAdapter> preTaskTypeAdapters,
                                          Multimap<String, TaskTreeAdapter> postTypeAdapters,
                                          Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers,
                                          FileCollection additionalRecompileDependencies) {
        super(project, "neoForm", version, side, preTaskTypeAdapters, postTypeAdapters, taskCustomizers, NeoFormRuntimeExtension.class);
        this.neoFormArchive = neoFormArchive;
        this.config = config;
        this.additionalRecompileDependencies = additionalRecompileDependencies;
    }

    public NeoFormConfigConfigurationSpecV2 getConfig() {
        return config;
    }

    public String getMinecraftVersion() {
        return config.getVersion();
    }
    
    public String getNeoFormVersion() {
        return getVersion();
    }

    public File getNeoFormArchive() {
        return neoFormArchive;
    }

    @Override
    public FileCollection getAdditionalRecompileDependencies() {
        return additionalRecompileDependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NeoFormRuntimeSpecification)) return false;
        if (!super.equals(o)) return false;

        NeoFormRuntimeSpecification spec = (NeoFormRuntimeSpecification) o;

        if (!neoFormArchive.equals(spec.neoFormArchive)) return false;
        return additionalRecompileDependencies.equals(spec.additionalRecompileDependencies);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + neoFormArchive.hashCode();
        result = 31 * result + additionalRecompileDependencies.hashCode();
        return result;
    }

    public static final class Builder extends CommonRuntimeSpecification.Builder<NeoFormRuntimeSpecification, Builder> implements NeoFormSpecification.Builder<NeoFormRuntimeSpecification, Builder> {

        private Dependency neoFormDependency;
        private FileCollection additionalDependencies;

        private Builder(Project project) {
            super(project);
            this.additionalDependencies = project.getObjects().fileCollection();
            withNeoFormVersion("+");
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public static Builder from(final Project project) {
            return new Builder(project);
        }

        @NotNull
        @Override
        public Builder withNeoFormVersion(@NotNull String version) {
            this.neoFormDependency = project.getDependencies().create("net.neoforged:neoform:" + version + "@zip");
            return getThis();
        }

        @NotNull
        @Override
        public Builder withNeoFormDependency(@NotNull Object notation) {
            this.neoFormDependency = getProject().getDependencies().create(notation);
            return getThis();
        }

        @Override
        public Builder withAdditionalDependencies(final FileCollection files) {
            this.additionalDependencies = this.additionalDependencies.plus(files);
            return getThis();
        }

        public NeoFormRuntimeSpecification build() {
            ResolvedArtifact artifact = ToolUtilities.resolveToolArtifact(project, neoFormDependency);
            File archive = artifact.getFile();
            String effectiveVersion = artifact.getModuleVersion().getId().getVersion();

            // Read the NF config from the archive
            NeoFormConfigConfigurationSpecV2 config;
            try {
                config = FileUtils.processFileFromZip(archive, "config.json", NeoFormConfigConfigurationSpecV2::get);
            } catch (IOException e) {
                throw new GradleException("Failed to read NeoForm config file from version " + effectiveVersion);
            }

            return new NeoFormRuntimeSpecification(
                    project,
                    effectiveVersion,
                    archive,
                    config,
                    distributionType.get(),
                    preTaskAdapters,
                    postTaskAdapters,
                    taskCustomizers,
                    additionalDependencies
            );
        }
    }
}
