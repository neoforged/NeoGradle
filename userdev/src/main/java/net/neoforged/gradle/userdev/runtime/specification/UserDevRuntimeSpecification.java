package net.neoforged.gradle.userdev.runtime.specification;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskCustomizer;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.Artifact;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.dsl.userdev.runtime.specification.UserDevSpecification;
import net.neoforged.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import net.neoforged.gradle.util.FileUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Defines a specification for a ForgeUserDev runtime.
 */
public final class UserDevRuntimeSpecification extends CommonRuntimeSpecification implements UserDevSpecification {

    private final FileTree userDevArchive;
    private final String forgeGroup;
    private final String forgeName;
    private final String forgeVersion;
    private final UserdevProfile profile;
    @Nullable
    private String minecraftVersion = null;

    public UserDevRuntimeSpecification(Project project,
                                       String version,
                                       FileTree userDevArchive,
                                       UserdevProfile profile,
                                       DistributionType distribution,
                                       Multimap<String, TaskTreeAdapter> preTaskTypeAdapters,
                                       Multimap<String, TaskTreeAdapter> postTypeAdapters,
                                       Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers,
                                       String forgeGroup,
                                       String forgeName,
                                       String forgeVersion) {
        super(project, "neoForge", version, distribution, preTaskTypeAdapters, postTypeAdapters, taskCustomizers, UserDevRuntimeExtension.class);
        this.userDevArchive = userDevArchive;
        this.profile = profile;
        this.forgeGroup = forgeGroup;
        this.forgeName = forgeName;
        this.forgeVersion = forgeVersion;
    }

    @Override
    public @NotNull String getForgeVersion() {
        return forgeVersion;
    }

    public FileTree getUserDevArchive() {
        return userDevArchive;
    }

    public String getForgeGroup() {
        return forgeGroup;
    }

    public String getForgeName() {
        return forgeName;
    }

    public UserdevProfile getProfile() {
        return profile;
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
        public Builder withForgeVersion(final Provider<String> forgeVersion) {
            this.forgeVersionProvider = forgeVersion;
            return this;
        }

        @Override
        public Builder withForgeVersion(final String forgeVersion) {
            if (forgeVersion == null) // Additional null check for convenient loading of versions from compileDependencies.
                return this;

            return withForgeVersion(project.provider(() -> forgeVersion));
        }

        @Override
        public Builder withForgeName(final Provider<String> mcpName) {
            this.forgeNameProvider = mcpName;
            return this;
        }

        @Override
        public Builder withForgeName(final String mcpName) {
            if (mcpName == null) // Additional null check for convenient loading of names from compileDependencies.
                return this;

            return withForgeName(project.provider(() -> mcpName));
        }

        @Override
        public Builder withForgeGroup(final Provider<String> mcpGroup) {
            this.forgeGroupProvider = mcpGroup;
            return this;
        }

        @Override
        public Builder withForgeGroup(final String mcpGroup) {
            if (mcpGroup == null) // Additional null check for convenient loading of groups from compileDependencies.
                return this;

            return withForgeGroup(project.provider(() -> mcpGroup));
        }

        public @NotNull UserDevRuntimeSpecification build() {
            final String group = forgeGroupProvider.get();
            final String name = forgeNameProvider.get();
            final String version = forgeVersionProvider.get();

            final Artifact artifact = new Artifact(group, name, version, "userdev", "jar");
            ResolvedArtifact userdevArchiveArtifact = ToolUtilities.resolveToolArtifact(project, artifact.getDescriptor());

            File userdevArchive = userdevArchiveArtifact.getFile();
            ModuleVersionIdentifier effectiveVersion = userdevArchiveArtifact.getModuleVersion().getId();

            // Read the userdev profile from the archive
            UserdevProfile profile;
            try {
                profile = FileUtils.processFileFromZip(userdevArchive, "config.json", in -> UserdevProfile.get(project.getObjects(), in));
            } catch (IOException e) {
                throw new GradleException("Failed to read userdev config file for version " + effectiveVersion, e);
            }

            return new UserDevRuntimeSpecification(
                    project,
                    effectiveVersion.getVersion(),
                    project.zipTree(userdevArchive),
                    profile,
                    distributionType.get(),
                    preTaskAdapters,
                    postTaskAdapters,
                    taskCustomizers,
                    effectiveVersion.getGroup(),
                    effectiveVersion.getName(),
                    effectiveVersion.getVersion()
            );
        }
    }
}
