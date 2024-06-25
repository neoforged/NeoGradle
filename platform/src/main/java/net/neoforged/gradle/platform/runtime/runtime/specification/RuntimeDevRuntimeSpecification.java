package net.neoforged.gradle.platform.runtime.runtime.specification;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskCustomizer;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.specification.NeoFormRuntimeSpecification;
import net.neoforged.gradle.platform.runtime.runtime.extension.RuntimeDevRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Defines a specification for a ForgeUserDev runtime.
 */
public final class RuntimeDevRuntimeSpecification extends CommonRuntimeSpecification {

    private final NeoFormRuntimeDefinition neoFormRuntime;
    private final Directory patchesDirectory;
    private final Directory rejectsDirectory;
    private final boolean isUpdating;
    
    public RuntimeDevRuntimeSpecification(NeoFormRuntimeDefinition neoFormRuntime,
                                          Multimap<String, TaskTreeAdapter> preTaskTypeAdapters,
                                          Multimap<String, TaskTreeAdapter> postTypeAdapters,
                                          Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers,
                                          Directory patchesDirectory,
                                          Directory rejectsDirectory,
                                          boolean isUpdating) {
        super(neoFormRuntime.getSpecification().getProject(),
                "platform",
                neoFormRuntime.getSpecification().getVersion(),
                neoFormRuntime.getSpecification().getDistribution(),
                preTaskTypeAdapters,
                postTypeAdapters,
                taskCustomizers,
                RuntimeDevRuntimeExtension.class);
        this.neoFormRuntime = neoFormRuntime;
        this.patchesDirectory = patchesDirectory;
        this.rejectsDirectory = rejectsDirectory;
        this.isUpdating = isUpdating;
    }

    public NeoFormRuntimeDefinition getNeoFormRuntime() {
        return neoFormRuntime;
    }

    @NotNull
    @Override
    public String getMinecraftVersion() {
        return neoFormRuntime.getSpecification().getMinecraftVersion();
    }

    public Directory getPatchesDirectory() {
        if (!patchesDirectory.getAsFile().exists()) {
            //Ensure the directory exists
            patchesDirectory.getAsFile().mkdirs();
        }

        return patchesDirectory;
    }
    
    public Directory getRejectsDirectory() {
        if (!rejectsDirectory.getAsFile().exists()) {
            //Ensure the directory exists
            rejectsDirectory.getAsFile().mkdirs();
        }
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
        RuntimeDevRuntimeSpecification that = (RuntimeDevRuntimeSpecification) o;
        return isUpdating == that.isUpdating && Objects.equals(neoFormRuntime, that.neoFormRuntime) && Objects.equals(patchesDirectory, that.patchesDirectory) && Objects.equals(rejectsDirectory, that.rejectsDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), neoFormRuntime, patchesDirectory, rejectsDirectory, isUpdating);
    }

    public static final class Builder extends CommonRuntimeSpecification.Builder<RuntimeDevRuntimeSpecification, Builder> {

        private NeoFormRuntimeDefinition neoFormRuntimeDefinition;
        private Provider<Directory> patchesDirectory;
        private Provider<Directory> rejectsDirectory;
        private Provider<Boolean> isUpdating;
        
        private Builder(Project project) {
            super(project);
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

        public Builder withNeoFormRuntime(NeoFormRuntimeDefinition neoFormRuntimeDefinition) {
            this.neoFormRuntimeDefinition = neoFormRuntimeDefinition;
            return getThis();
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

        public @NotNull RuntimeDevRuntimeSpecification build() {
            if (neoFormRuntimeDefinition == null) {
                throw new IllegalStateException("Setting a neoFormRuntimeDefinition is required");
            }

            NeoFormRuntimeSpecification neoFormSpec = neoFormRuntimeDefinition.getSpecification();
            if (neoFormSpec.getProject() != getProject()) {
                throw new IllegalStateException("Cannot use a neoFormRuntimeDefinition from a different project (" + neoFormSpec.getProject() + ")");
            }

            if (distributionType.get() != neoFormSpec.getDistribution()){
                throw new IllegalStateException("Cannot change the distribution type to " + distributionType.get() +
                        " if the NeoForm runtime is for " + neoFormSpec.getDistribution());
            }

            return new RuntimeDevRuntimeSpecification(
                    neoFormRuntimeDefinition,
                    preTaskAdapters,
                    postTaskAdapters,
                    taskCustomizers,
                    patchesDirectory.get(),
                    rejectsDirectory.get(),
                    isUpdating.get());
        }
    }
}
