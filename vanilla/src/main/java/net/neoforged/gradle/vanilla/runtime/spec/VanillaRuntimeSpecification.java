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

import java.util.Optional;

/**
 * Defines a specification for a vanilla runtime.
 */
public final class VanillaRuntimeSpecification extends CommonRuntimeSpecification implements VanillaSpecification {
    private final String minecraftVersion;
    private final String fartVersion;
    private final String forgeFlowerVersion;
    private final String accessTransformerApplierVersion;

    public VanillaRuntimeSpecification(Project project,
                                       String name,
                                       String version,
                                       DistributionType side,
                                       Multimap<String, TaskTreeAdapter> preTaskTypeAdapters,
                                       Multimap<String, TaskTreeAdapter> postTypeAdapters,
                                       Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers,
                                       String minecraftVersion,
                                       String fartVersion,
                                       String forgeFlowerVersion,
                                       String accessTransformerApplierVersion) {
        super(project, name, version, side, preTaskTypeAdapters, postTypeAdapters, taskCustomizers, VanillaRuntimeExtension.class, Usage.FULL);
        this.minecraftVersion = minecraftVersion;
        this.fartVersion = fartVersion;
        this.forgeFlowerVersion = forgeFlowerVersion;
        this.accessTransformerApplierVersion = accessTransformerApplierVersion;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    @Override
    public String getFartVersion() {
        return fartVersion;
    }

    @Override
    public String getForgeFlowerVersion() {
        return forgeFlowerVersion;
    }

    @Override
    public String getAccessTransformerApplierVersion() {
        return accessTransformerApplierVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VanillaRuntimeSpecification)) return false;
        if (!super.equals(o)) return false;

        VanillaRuntimeSpecification that = (VanillaRuntimeSpecification) o;

        if (!minecraftVersion.equals(that.minecraftVersion)) return false;
        if (!fartVersion.equals(that.fartVersion)) return false;
        if (!forgeFlowerVersion.equals(that.forgeFlowerVersion)) return false;
        return accessTransformerApplierVersion.equals(that.accessTransformerApplierVersion);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + minecraftVersion.hashCode();
        result = 31 * result + fartVersion.hashCode();
        result = 31 * result + forgeFlowerVersion.hashCode();
        result = 31 * result + accessTransformerApplierVersion.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "VanillaRuntimeSpec{" +
                "minecraftVersion='" + minecraftVersion + '\'' +
                ", fartVersion='" + fartVersion + '\'' +
                ", forgeFlowerVersion='" + forgeFlowerVersion + '\'' +
                ", accessTransformerApplierVersion='" + accessTransformerApplierVersion + '\'' +
                '}';
    }

    public static final class Builder extends CommonRuntimeSpecification.Builder<VanillaRuntimeSpecification, Builder> {

        private Provider<String> minecraftArtifact;
        
        private Provider<String> minecraftVersion;

        private Provider<String> fartVersion;
        private boolean hasConfiguredFartVersion = false;

        private Provider<String> forgeFlowerVersion;
        private boolean hasConfiguredForgeFlowerVersion = false;

        private Provider<String> accessTransformerApplierVersion;
        private boolean hasConfiguredAccessTransformerApplierVersion = false;

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

        @Override
        protected void configureBuilder() {
            super.configureBuilder();
            final VanillaRuntimeExtension runtimeExtension = this.getProject().getExtensions().getByType(VanillaRuntimeExtension.class);

            if (!this.hasConfiguredFartVersion) {
                this.fartVersion = runtimeExtension.getFartVersion();
            }

            if (!this.hasConfiguredForgeFlowerVersion) {
                this.forgeFlowerVersion = runtimeExtension.getVineFlowerVersion();
            }

            if (!this.hasConfiguredAccessTransformerApplierVersion) {
                this.accessTransformerApplierVersion = runtimeExtension.getAccessTransformerApplierVersion();
            }
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

        public Builder withFartVersion(final Provider<String> fartVersion) {
            this.fartVersion = fartVersion;
            this.hasConfiguredFartVersion = true;
            return getThis();
        }

        public Builder withFartVersion(final String fartVersion) {
            if (fartVersion == null) // Additional null check for convenient loading of sides from dependencies.
                return getThis();

            return withFartVersion(project.provider(() -> fartVersion));
        }

        public Builder withForgeFlowerVersion(final Provider<String> forgeFlowerVersion) {
            this.forgeFlowerVersion = forgeFlowerVersion;
            this.hasConfiguredForgeFlowerVersion = true;
            return getThis();
        }

        public Builder withForgeFlowerVersion(final String forgeFlowerVersion) {
            if (forgeFlowerVersion == null) // Additional null check for convenient loading of sides from dependencies.
                return getThis();

            return withForgeFlowerVersion(project.provider(() -> forgeFlowerVersion));
        }

        public Builder withAccessTransformerApplierVersion(final Provider<String> accessTransformerApplierVersion) {
            this.accessTransformerApplierVersion = accessTransformerApplierVersion;
            this.hasConfiguredAccessTransformerApplierVersion = true;
            return getThis();
        }

        public Builder withAccessTransformerApplierVersion(final String accessTransformerApplierVersion) {
            if (accessTransformerApplierVersion == null) // Additional null check for convenient loading of sides from dependencies.
                return getThis();

            return withAccessTransformerApplierVersion(project.provider(() -> accessTransformerApplierVersion));
        }

        @Override
        public VanillaRuntimeSpecification build() {
            return new VanillaRuntimeSpecification(
                    project,
                    minecraftArtifact.get(),
                    Optional.of(minecraftVersion.get()).map(v -> v.equals("+") ? "" : v).get(),
                    distributionType.get(),
                    preTaskAdapters,
                    postTaskAdapters,
                    taskCustomizers,
                    minecraftVersion.get(),
                    fartVersion.get(),
                    forgeFlowerVersion.get(),
                    accessTransformerApplierVersion.get());
        }
    }
}
