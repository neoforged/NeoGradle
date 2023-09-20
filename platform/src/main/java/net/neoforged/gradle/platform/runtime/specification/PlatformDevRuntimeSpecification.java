package net.neoforged.gradle.platform.runtime.specification;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.Artifact;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.platform.runtime.extension.PlatformDevRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Defines a specification for a ForgeUserDev runtime.
 */
public final class PlatformDevRuntimeSpecification extends CommonRuntimeSpecification {
    
    private final Artifact neoFormArtifact;
    private final String minecraftVersion;
    private final FileCollection additionalDependencies;
    private final Directory patchesDirectory;
    private final Directory rejectsDirectory;
    private final boolean isUpdating;
    
    public PlatformDevRuntimeSpecification(Project project, DistributionType distribution, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters, Artifact neoFormArtifact, FileCollection additionalDependencies, Directory patchesDirectory, Directory rejectsDirectory, boolean isUpdating) {
        super(project, "platform", neoFormArtifact.getVersion(), distribution, preTaskTypeAdapters, postTypeAdapters, PlatformDevRuntimeExtension.class);
        this.minecraftVersion = neoFormArtifact.getVersion().substring(0, neoFormArtifact.getVersion().lastIndexOf("-"));
        this.neoFormArtifact = neoFormArtifact;
        this.additionalDependencies = additionalDependencies;
        this.patchesDirectory = patchesDirectory;
        this.rejectsDirectory = rejectsDirectory;
        this.isUpdating = isUpdating;
    }
    
    @NotNull
    @Override
    public String getMinecraftVersion() {
        return Objects.requireNonNull(minecraftVersion, "Minecraft version not set");
    }
    
    public Artifact getNeoFormArtifact() {
        return neoFormArtifact;
    }
    
    public FileCollection getAdditionalDependencies() {
        return additionalDependencies;
    }
    
    public Directory getPatchesDirectory() {
        return patchesDirectory;
    }
    
    public Directory getRejectsDirectory() {
        return rejectsDirectory;
    }
    
    public boolean isUpdating() {
        return isUpdating;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PlatformDevRuntimeSpecification that = (PlatformDevRuntimeSpecification) o;
        return Objects.equals(neoFormArtifact, that.neoFormArtifact) && Objects.equals(minecraftVersion, that.minecraftVersion) && Objects.equals(additionalDependencies, that.additionalDependencies) && Objects.equals(patchesDirectory, that.patchesDirectory) && Objects.equals(rejectsDirectory, that.rejectsDirectory);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), neoFormArtifact, minecraftVersion, additionalDependencies, patchesDirectory, rejectsDirectory);
    }
    
    public static final class Builder extends CommonRuntimeSpecification.Builder<PlatformDevRuntimeSpecification, Builder> {
        
        private Provider<String> neoFormGroup;
        private Provider<String> neoFormName;
        private Provider<String> neoFormVersion;
        private Provider<Artifact> neoFormArtifact;
        private FileCollection additionalDependencies;
        private Provider<Directory> patchesDirectory;
        private Provider<Directory> rejectsDirectory;
        private Provider<Boolean> isUpdating;
        
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
            
            this.patchesDirectory = project.provider(() -> project.getLayout().getProjectDirectory().dir("patches"));
            this.rejectsDirectory = project.provider(() -> project.getLayout().getProjectDirectory().dir("rejects"));
            this.isUpdating = project.provider(() -> false);
        }
        
        @Override
        protected Builder getThis() {
            return this;
        }
        
        public static Builder from(final Project project) {
            return new Builder(project);
        }
        
        public Builder withNeoFormGroup(final Provider<String> neoFormGroup) {
            this.neoFormGroup = neoFormGroup;
            return getThis();
        }
        
        public Builder withNeoFormGroup(final String neoFormGroup) {
            if (neoFormGroup == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();
            
            return withNeoFormGroup(project.provider(() -> neoFormGroup));
        }
        
        public Builder withNeoFormName(final Provider<String> neoFormName) {
            this.neoFormName = neoFormName;
            return getThis();
        }
        
        public Builder withNeoFormName(final String neoFormName) {
            if (neoFormName == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();
            
            return withNeoFormName(project.provider(() -> neoFormName));
        }
        
        public Builder withNeoFormVersion(final Provider<String> neoFormVersion) {
            this.neoFormVersion = neoFormVersion;
            return getThis();
        }
        
        public Builder withNeoFormVersion(final String neoFormVersion) {
            if (neoFormVersion == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();
            
            return withNeoFormVersion(project.provider(() -> neoFormVersion));
        }
        
        public Builder withNeoFormArtifact(final Provider<Artifact> neoFormArtifact) {
            this.neoFormArtifact = neoFormArtifact;
            return getThis();
        }
        
        public Builder withNeoFormArtifact(final Artifact neoFormArtifact) {
            if (neoFormArtifact == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();
            
            return withNeoFormArtifact(project.provider(() -> neoFormArtifact));
        }
        
        public Builder withPatchesDirectory(final Provider<Directory> patchesDirectory) {
            this.patchesDirectory = patchesDirectory;
            return getThis();
        }
        
        public Builder withPatchesDirectory(final Directory patchesDirectory) {
            if (patchesDirectory == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();
            
            return withPatchesDirectory(project.provider(() -> patchesDirectory));
        }
        
        public Builder withRejectsDirectory(final Provider<Directory> rejectsDirectory) {
            this.rejectsDirectory = rejectsDirectory;
            return getThis();
        }
        
        public Builder withRejectsDirectory(final Directory rejectsDirectory) {
            if (rejectsDirectory == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();
            
            return withRejectsDirectory(project.provider(() -> rejectsDirectory));
        }
        
        public Builder isUpdating(final Provider<Boolean> isUpdating) {
            this.isUpdating = isUpdating;
            return getThis();
        }
        
        public Builder isUpdating(final Boolean isUpdating) {
            if (isUpdating == null) // Additional null check for convenient loading of versions from dependencies.
                return getThis();
            
            return isUpdating(project.provider(() -> isUpdating));
        }
        
        
        public Builder withAdditionalDependencies(final FileCollection files) {
            this.additionalDependencies = this.additionalDependencies.plus(files);
            return getThis();
        }
        
        public @NotNull PlatformDevRuntimeSpecification build() {
            final Provider<Artifact> resolvedArtifact = neoFormArtifact.map(a -> resolveNeoFormVersion(project, a));
            
            return new PlatformDevRuntimeSpecification(
                    project,
                    distributionType.get(),
                    preTaskAdapters,
                    postTaskAdapters,
                    resolvedArtifact.get(),
                    additionalDependencies,
                    patchesDirectory.get(),
                    rejectsDirectory.get(),
                    isUpdating.get());
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
