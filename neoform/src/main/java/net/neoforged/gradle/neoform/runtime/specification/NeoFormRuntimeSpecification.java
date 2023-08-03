package net.neoforged.gradle.neoform.runtime.specification;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.Artifact;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.neoform.runtime.specification.NeoFormSpecification;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;

/**
 * Defines a specification for an MCP runtime.
 */
public class NeoFormRuntimeSpecification extends CommonRuntimeSpecification implements NeoFormSpecification {
    private static final long serialVersionUID = -3537760562547500214L;
    private final Artifact neoFormArtifact;
    private final FileCollection additionalRecompileDependencies;

    public NeoFormRuntimeSpecification(Project project, String name, Artifact neoFormArtifact, DistributionType side, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters, FileCollection additionalRecompileDependencies) {
        super(project, name, side, preTaskTypeAdapters, postTypeAdapters);
        this.neoFormArtifact = neoFormArtifact;
        this.additionalRecompileDependencies = additionalRecompileDependencies;
    }

    public String getMinecraftVersion() {
        final String fromArtifactVersion = getNeoFormArtifact().getVersion().split("-")[0];
        final MinecraftArtifactCache cache = getProject().getExtensions().getByType(MinecraftArtifactCache.class);
        return cache.resolveVersion(fromArtifactVersion);
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
            return new NeoFormRuntimeSpecification(project, namePrefix, neoFormArtifact.get(), distributionType.get(), preTaskAdapters, postTaskAdapters, additionalDependencies);
        }
    }
}
