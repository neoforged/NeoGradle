package net.neoforged.gradle.userdev.runtime.specification;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.userdev.extension.UserDev;
import net.neoforged.gradle.dsl.userdev.runtime.specification.UserDevSpecification;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Defines a specification for a ForgeUserDev runtime.
 */
public final class UserDevRuntimeSpecification extends CommonRuntimeSpecification implements UserDevSpecification {

    private final String forgeGroup;
    private final String forgeName;
    private final String forgeVersion;
    @Nullable
    private String minecraftVersion = null;

    public UserDevRuntimeSpecification(Project project, String name, DistributionType distribution, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters, String forgeGroup, String forgeName, String forgeVersion) {
        super(project, name, distribution, preTaskTypeAdapters, postTypeAdapters);
        this.forgeGroup = forgeGroup;
        this.forgeName = forgeName;
        this.forgeVersion = forgeVersion;
    }

    @Override
    public String getForgeVersion() {
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

    public void setMinecraftVersion(String minecraftVersion) {
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
        public Builder withForgeVersion(final Provider<String> mcpVersion) {
            this.forgeVersionProvider = mcpVersion;
            this.hasConfiguredForgeVersion = true;
            return this;
        }

        @Override
        public Builder withForgeVersion(final String mcpVersion) {
            if (mcpVersion == null) // Additional null check for convenient loading of versions from dependencies.
                return this;

            return withForgeVersion(project.provider(() -> mcpVersion));
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
            return new UserDevRuntimeSpecification(project, namePrefix, distributionType.get(), preTaskAdapters, postTaskAdapters, forgeGroupProvider.get(), forgeNameProvider.get(), forgeVersionProvider.get());
        }
    }
}