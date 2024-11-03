package net.neoforged.gradle.vanilla.runtime.spec;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskCustomizer;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.vanilla.runtime.spec.VanillaSpecification;
import net.neoforged.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Defines a specification for a vanilla runtime.
 */
public final class VanillaRuntimeSpecification extends CommonRuntimeSpecification implements VanillaSpecification {
    private final String minecraftVersion;

    public VanillaRuntimeSpecification(Project project,
                                       String name,
                                       String version,
                                       DistributionType side,
                                       Multimap<String, TaskTreeAdapter> preTaskTypeAdapters,
                                       Multimap<String, TaskTreeAdapter> postTypeAdapters,
                                       Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers,
                                       String minecraftVersion) {
        super(project, name, version, side, preTaskTypeAdapters, postTypeAdapters, taskCustomizers, VanillaRuntimeExtension.class);
        this.minecraftVersion = minecraftVersion;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VanillaRuntimeSpecification that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getMinecraftVersion(), that.getMinecraftVersion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMinecraftVersion());
    }

    @Override
    public String toString() {
        return "VanillaRuntimeSpecification{" +
                "minecraftVersion='" + minecraftVersion + '\'' +
                '}';
    }

    public static final class Builder extends CommonRuntimeSpecification.Builder<VanillaRuntimeSpecification, Builder> {

        private Provider<String> minecraftArtifact;
        
        private Provider<String> minecraftVersion;

        public static Builder from(Project project) {
            return new Builder(project);
        }

        private Builder(Project project) {
            super(project);
            
            withMinecraftArtifact("client");
            withDistributionType(DistributionType.CLIENT);
        }

        @Override
        protected Builder getThis() {
            return this;
        }


        public Builder withMinecraftArtifact(final Provider<String> minecraftArtifact) {
            this.minecraftArtifact = minecraftArtifact;
            return getThis();
        }

        public Builder withMinecraftArtifact(final String minecraftArtifact) {
            if (minecraftArtifact == null) // Additional null check for convenient loading of sides from dependencies.
                return getThis();

            return withMinecraftArtifact(project.provider(() -> minecraftArtifact));
        }

        public Builder withMinecraftVersion(final Provider<String> minecraftVersion) {
            this.minecraftVersion = minecraftVersion;
            return getThis();
        }

        public Builder withMinecraftVersion(final String minecraftVersion) {
            if (minecraftVersion == null) // Additional null check for convenient loading of sides from dependencies.
                return getThis();

            return withMinecraftVersion(project.provider(() -> minecraftVersion));
        }

        @Override
        public @NotNull VanillaRuntimeSpecification build() {
            return new VanillaRuntimeSpecification(
                    project,
                    minecraftArtifact.get(),
                    Optional.of(minecraftVersion.get()).map(v -> v.equals("+") ? "" : v).get(),
                    distributionType.get(),
                    preTaskAdapters,
                    postTaskAdapters,
                    taskCustomizers,
                    minecraftVersion.get()
            );
        }
    }
}
