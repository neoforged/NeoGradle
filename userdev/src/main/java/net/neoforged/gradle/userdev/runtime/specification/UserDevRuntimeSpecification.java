package net.neoforged.gradle.userdev.runtime.specification;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.Artifact;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.userdev.extension.UserDev;
import net.neoforged.gradle.dsl.userdev.runtime.specification.UserDevSpecification;
import net.neoforged.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Defines a specification for a ForgeUserDev runtime.
 */
public final class UserDevRuntimeSpecification extends CommonRuntimeSpecification implements UserDevSpecification {

    private final String forgeGroup;
    private final String forgeName;
    private final String forgeVersion;
    @Nullable
    private String minecraftVersion = null;

    public UserDevRuntimeSpecification(Project project, String version, DistributionType distribution, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters, String forgeGroup, String forgeName, String forgeVersion) {
        super(project, "forge", version, distribution, preTaskTypeAdapters, postTypeAdapters, UserDevRuntimeExtension.class);
        this.forgeGroup = forgeGroup;
        this.forgeName = forgeName;
        this.forgeVersion = forgeVersion;
    }

    @Override
    public @NotNull String getForgeVersion() {
        return forgeVersion;
    }

    public String getForgeGroup() {
        return forgeGroup;
    }

    public String getForgeName() {
        return forgeName;
    }

    @NotNull
    @Override
    public String getMinecraftVersion() {
        return Objects.requireNonNull(minecraftVersion, "Minecraft version not set");
    }

    public void setMinecraftVersion(@NotNull String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UserDevRuntimeSpecification that = (UserDevRuntimeSpecification) o;
        return Objects.equals(forgeGroup, that.forgeGroup) && Objects.equals(forgeName, that.forgeName) && Objects.equals(forgeVersion, that.forgeVersion) && Objects.equals(minecraftVersion, that.minecraftVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), forgeGroup, forgeName, forgeVersion, minecraftVersion);
    }

    public static final class Builder extends CommonRuntimeSpecification.Builder<UserDevRuntimeSpecification, Builder> implements UserDevSpecification.Builder<UserDevRuntimeSpecification, Builder> {

        private boolean hasConfiguredForgeVersion;
        private boolean hasConfiguredForgeName;
        private boolean hasConfiguredForgeGroup;
        private Provider<String> forgeVersionProvider;
        private Provider<String> forgeGroupProvider;
        private Provider<String> forgeNameProvider;

        private Builder(Project project) {
            super(project);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        public static Builder from(final Project project) {
            return new Builder(project);
        }

        @Override
        protected void configureBuilder() {
            super.configureBuilder();
            final UserDev runtimeExtension = getProject().getExtensions().getByType(UserDev.class);

            if (!hasConfiguredForgeVersion) {
                forgeVersionProvider = runtimeExtension.getDefaultForgeVersion();
            }

            if (!hasConfiguredForgeGroup) {
                forgeGroupProvider = runtimeExtension.getDefaultForgeGroup();
            }

            if (!hasConfiguredForgeName) {
                forgeNameProvider = runtimeExtension.getDefaultForgeName();
            }
        }

        @Override
        public Builder withForgeVersion(final Provider<String> forgeVersion) {
            this.forgeVersionProvider = forgeVersion;
            this.hasConfiguredForgeVersion = true;
            return this;
        }

        @Override
        public Builder withForgeVersion(final String forgeVersion) {
            if (forgeVersion == null) // Additional null check for convenient loading of versions from dependencies.
                return this;

            return withForgeVersion(project.provider(() -> forgeVersion));
        }

        @Override
        public Builder withForgeName(final Provider<String> mcpName) {
            this.forgeNameProvider = mcpName;
            this.hasConfiguredForgeName = true;
            return this;
        }

        @Override
        public Builder withForgeName(final String mcpName) {
            if (mcpName == null) // Additional null check for convenient loading of names from dependencies.
                return this;

            return withForgeName(project.provider(() -> mcpName));
        }

        @Override
        public Builder withForgeGroup(final Provider<String> mcpGroup) {
            this.forgeGroupProvider = mcpGroup;
            this.hasConfiguredForgeGroup = true;
            return this;
        }

        @Override
        public Builder withForgeGroup(final String mcpGroup) {
            if (mcpGroup == null) // Additional null check for convenient loading of groups from dependencies.
                return this;

            return withForgeGroup(project.provider(() -> mcpGroup));
        }

        public UserDevRuntimeSpecification build() {
            final String group = forgeGroupProvider.get();
            final String name = forgeNameProvider.get();
            final String version = forgeVersionProvider.get();

            final Artifact universalArtifact = new Artifact(group, name, version, "userdev", "jar");
            final Artifact resolvedArtifact = resolveUserDevVersion(project, universalArtifact);

            return new UserDevRuntimeSpecification(project, resolvedArtifact.getVersion(), distributionType.get(), preTaskAdapters, postTaskAdapters, resolvedArtifact.getGroup(), resolvedArtifact.getName(), resolvedArtifact.getVersion());
        }

        private static Artifact resolveUserDevVersion(final Project project, final Artifact current) {
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
