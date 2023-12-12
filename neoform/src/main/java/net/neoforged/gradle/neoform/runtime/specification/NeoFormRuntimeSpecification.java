package net.neoforged.gradle.neoform.runtime.specification;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskCustomizer;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.Artifact;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.neoform.runtime.specification.NeoFormSpecification;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;

import java.util.Objects;

/**
 * Defines a specification for an MCP runtime.
 */
public class NeoFormRuntimeSpecification extends CommonRuntimeSpecification implements NeoFormSpecification {
    private final Artifact neoFormArtifact;
    private final FileCollection additionalRecompileDependencies;

    public NeoFormRuntimeSpecification(Project project,
                                       String version,
                                       Artifact neoFormArtifact,
                                       DistributionType side,
                                       Multimap<String, TaskTreeAdapter> preTaskTypeAdapters,
                                       Multimap<String, TaskTreeAdapter> postTypeAdapters,
                                       Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers,
                                       FileCollection additionalRecompileDependencies) {
        super(project, "neoForm", version, side, preTaskTypeAdapters, postTypeAdapters, taskCustomizers, NeoFormRuntimeExtension.class);
        this.neoFormArtifact = neoFormArtifact;
        this.additionalRecompileDependencies = additionalRecompileDependencies;
    }

    public String getMinecraftVersion() {
        return getNeoFormArtifact().getVersion().substring(0, getNeoFormArtifact().getVersion().lastIndexOf("-"));
    }
    
    public String getNeoFormVersion() {
        return getNeoFormArtifact().getVersion().substring(getNeoFormArtifact().getVersion().lastIndexOf("-") + 1);
    }

    @Override
    public Artifact getNeoFormArtifact() {
        return neoFormArtifact;
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

        if (!neoFormArtifact.equals(spec.neoFormArtifact)) return false;
        return additionalRecompileDependencies.equals(spec.additionalRecompileDependencies);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + neoFormArtifact.hashCode();
        result = 31 * result + additionalRecompileDependencies.hashCode();
        return result;
    }

    public static final class Builder extends CommonRuntimeSpecification.Builder<NeoFormRuntimeSpecification, Builder> implements NeoFormSpecification.Builder<NeoFormRuntimeSpecification, Builder> {

        private Provider<String> neoFormGroup;
        private Provider<String> neoFormName;
        private Provider<String> neoFormVersion;
        private Provider<Artifact> neoFormArtifact;
        private FileCollection additionalDependencies;

        private Builder(Project project) {
            super(project);
            this.additionalDependencies = project.getObjects().fileCollection();

            this.neoFormGroup = project.provider(() -> "net.neoforged");
            this.neoFormName = project.provider(() -> "neoform");
            this.neoFormVersion = project.provider(() -> "+");

            this.neoFormArtifact = this.neoFormGroup
                    .flatMap(group -> this.neoFormName
                            .flatMap(name -> this.neoFormVersion
                                    .map(version -> Artifact.from(
                                            String.format("%s:%s:%s@zip", group, name, version)
                                    ))));
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public static Builder from(final Project project) {
            return new Builder(project);
        }

        @Override
        public Builder withNeoFormGroup(final Provider<String> neoFormGroup) {
            this.neoFormGroup = neoFormGroup;
            return getThis();
        }

        @Override
        public Builder withNeoFormGroup(final String neoFormGroup) {
            if (neoFormGroup == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();

            return withNeoFormGroup(project.provider(() -> neoFormGroup));
        }

        @Override
        public Builder withNeoFormName(final Provider<String> neoFormName) {
            this.neoFormName = neoFormName;
            return getThis();
        }

        @Override
        public Builder withNeoFormName(final String neoFormName) {
            if (neoFormName == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();

            return withNeoFormName(project.provider(() -> neoFormName));
        }

        @Override
        public Builder withNeoFormVersion(final Provider<String> neoFormVersion) {
            this.neoFormVersion = neoFormVersion;
            return getThis();
        }

        @Override
        public Builder withNeoFormVersion(final String neoFormVersion) {
            if (neoFormVersion == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();

            return withNeoFormVersion(project.provider(() -> neoFormVersion));
        }

        @Override
        public Builder withNeoFormArtifact(final Provider<Artifact> neoFormArtifact) {
            this.neoFormArtifact = neoFormArtifact;
            return getThis();
        }

        @Override
        public Builder withNeoFormArtifact(final Artifact neoFormArtifact) {
            if (neoFormArtifact == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();

            return withNeoFormArtifact(project.provider(() -> neoFormArtifact));
        }

        @Override
        public Builder withAdditionalDependencies(final FileCollection files) {
            this.additionalDependencies = this.additionalDependencies.plus(files);
            return getThis();
        }

        public NeoFormRuntimeSpecification build() {
            final Provider<Artifact> resolvedArtifact = neoFormArtifact.map(a -> resolveNeoFormVersion(project, a));
            final Provider<String> resolvedVersion = resolvedArtifact.map(Artifact::getVersion).map(v -> v.equals("+") ? "" : v);

            return new NeoFormRuntimeSpecification(
                    project,
                    resolvedVersion.get(),
                    resolvedArtifact.get(),
                    distributionType.get(),
                    preTaskAdapters,
                    postTaskAdapters,
                    taskCustomizers,
                    additionalDependencies
            );
        }

        private static Artifact resolveNeoFormVersion(final Project project, final Artifact current) {
            if (!Objects.equals(current.getVersion(), "+")) {
                return current;
            }

            final Configuration resolveConfig = ConfigurationUtils.temporaryConfiguration(project, current.toDependency(project));
            return resolveConfig.getResolvedConfiguration()
                    .getResolvedArtifacts().stream()
                    .filter(current.asArtifactMatcher())
                    .findFirst()
                    .map(Artifact::from).orElse(current);
        }
    }
}
