package net.neoforged.gradle.vanilla.util;

import net.neoforged.gradle.common.util.ConfigurationUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.attributes.Attribute;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("ConstantValue")
public class MinecraftDependencyUtil
{

    @Nullable
    public static String resolveMinecraftDependenciesVersion(final Project project, final ExternalModuleDependency dependency)
    {
        final Dependency minecraftDependenciesDependency = project.getDependencies().create("net.neoforged:minecraft-dependencies" + (
            dependency.getVersion() != null ? ":" + dependency.getVersion() : ""
        ));

        AtomicBoolean configured = new AtomicBoolean(dependency.getVersion() != null);
        if (minecraftDependenciesDependency instanceof ExternalModuleDependency emd)
        {
            emd.version(version -> {
                final VersionConstraint versionConstraint = dependency.getVersionConstraint();

                if (versionConstraint.getPreferredVersion() != null && !versionConstraint.getPreferredVersion().isEmpty())
                {
                    version.prefer(versionConstraint.getPreferredVersion());
                    configured.set(true);
                }

                if (versionConstraint.getRejectedVersions() != null && !versionConstraint.getRejectedVersions().isEmpty())
                {
                    version.reject(versionConstraint.getRejectedVersions().toArray(new String[0]));
                    configured.set(true);
                }

                if (versionConstraint.getRequiredVersion() != null && !versionConstraint.getRequiredVersion().isEmpty())
                {
                    version.require(versionConstraint.getRequiredVersion());
                    configured.set(true);
                }

                if (versionConstraint.getStrictVersion() != null && !versionConstraint.getStrictVersion().isEmpty())
                {
                    version.strictly(versionConstraint.getStrictVersion());
                    configured.set(true);
                }

                if (versionConstraint.getBranch() != null && !versionConstraint.getBranch().isEmpty())
                {
                    version.setBranch(versionConstraint.getBranch());
                    configured.set(true);
                }
            });
        }

        if (!configured.get())
        {
            return null;
        }

        final Configuration dependenciesLookup =
            ConfigurationUtils.temporaryUnhandledConfiguration(project.getConfigurations(), "ResolveRequestedMinecraftVersion", minecraftDependenciesDependency);

        dependenciesLookup.attributes(attributes -> {
            attributes.attribute(Attribute.of("net.neoforged.distribution", String.class), "client"); //The distribution does not matter here.
            attributes.attribute(Attribute.of("net.neoforged.operatingsystem", String.class), "linux"); //Again the OS does not matter, we only need the resolved version, and it will always exist for all of them
        });

        return dependenciesLookup.getResolvedConfiguration().getFirstLevelModuleDependencies().iterator().next().getModuleVersion();
    }
}
