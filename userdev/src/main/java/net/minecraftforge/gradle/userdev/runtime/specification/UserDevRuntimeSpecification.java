package net.minecraftforge.gradle.userdev.runtime.specification;

import com.google.common.collect.Multimap;
import net.minecraftforge.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import net.minecraftforge.gradle.dsl.userdev.extension.UserDev;
import net.minecraftforge.gradle.dsl.userdev.runtime.specification.UserDevSpecification;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Defines a specification for a ForgeUserDev runtime.
 */
public final class UserDevRuntimeSpecification extends CommonRuntimeSpecification implements UserDevSpecification {
    private final String forgeVersion;
    @Nullable
    private String minecraftVersion = null;

    public UserDevRuntimeSpecification(Project project, String name, DistributionType distribution, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters, String forgeVersion) {
        super(project, name, distribution, preTaskTypeAdapters, postTypeAdapters);
        this.forgeVersion = forgeVersion;
    }

    @Override
    public String getForgeVersion() {
        return forgeVersion;
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
        if (!(o instanceof UserDevRuntimeSpecification)) return false;
        if (!super.equals(o)) return false;

        UserDevRuntimeSpecification that = (UserDevRuntimeSpecification) o;

        if (!getForgeVersion().equals(that.getForgeVersion())) return false;
        return getMinecraftVersion().equals(that.getMinecraftVersion());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getForgeVersion().hashCode();
        result = 31 * result + getMinecraftVersion().hashCode();
        return result;
    }

    public static final class Builder extends CommonRuntimeSpecification.Builder<UserDevRuntimeSpecification, Builder> implements UserDevSpecification.Builder<UserDevRuntimeSpecification, Builder> {

        private boolean hasConfiguredForgeVersion;
        private Provider<String> forgeVersionProvider;

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

        public UserDevRuntimeSpecification build() {
            return new UserDevRuntimeSpecification(project, namePrefix, distributionType.get(), preTaskAdapters, postTaskAdapters, forgeVersionProvider.get());
        }
    }
}
