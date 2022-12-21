package net.minecraftforge.gradle.userdev.dependency;

import com.google.common.collect.Sets;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementsExtension;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.userdev.runtime.ForgeUserDevRuntimeDefinition;
import net.minecraftforge.gradle.userdev.runtime.extension.ForgeUserDevRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;

import java.util.Optional;
import java.util.Set;

public final class ForgeUserDevDependencyManager {
    private static final ForgeUserDevDependencyManager INSTANCE = new ForgeUserDevDependencyManager();

    public static ForgeUserDevDependencyManager getInstance() {
        return INSTANCE;
    }

    private ForgeUserDevDependencyManager() {
    }

    public void apply(final Project project) {
        final DependencyReplacement dependencyReplacer = project.getExtensions().getByType(DependencyReplacement.class);
        dependencyReplacer.getReplacementHandlers().add(context -> {
            if (isNotAMatchingDependency(context.dependency())) {
                return Optional.empty();
            }

            if (!(context.dependency() instanceof ExternalModuleDependency)) {
                return Optional.empty();
            }

            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) context.dependency();

            final ForgeUserDevRuntimeDefinition runtimeDefinition = buildForgeUserDevRuntimeFrom(project, context.project(), externalModuleDependency);

            final Set<Dependency> additionalDependencies = Sets.newHashSet();
            additionalDependencies.addAll(runtimeDefinition.mcpRuntimeDefinition().minecraftDependenciesConfiguration().getDependencies());
            additionalDependencies.addAll(runtimeDefinition.additionalUserDevDependencies().getDependencies());
            final Configuration additionalDependenciesConfiguration = project.getConfigurations().detachedConfiguration(additionalDependencies.toArray(new Dependency[0]));

            return Optional.of(
                    new DependencyReplacementResult(
                            project,
                            name -> CommonRuntimeUtils.buildTaskName(runtimeDefinition.mcpRuntimeDefinition(), name),
                            runtimeDefinition.mcpRuntimeDefinition().sourceJarTask(),
                            runtimeDefinition.mcpRuntimeDefinition().rawJarTask(),
                            additionalDependenciesConfiguration,
                            builder -> builder.setVersion(runtimeDefinition.spec().forgeVersion()),
                            runtimeDefinition::)
            );
        });
    }


    private boolean isNotAMatchingDependency(final Dependency dependencyToCheck) {
        if (dependencyToCheck instanceof ExternalModuleDependency) {
            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependencyToCheck;
            return externalModuleDependency.getGroup() == null || !externalModuleDependency.getGroup().equals("net.minecraftforge") || !isSupportedSide(dependencyToCheck) || !hasMatchingArtifact(externalModuleDependency);
        }

        return true;
    }

    private boolean isSupportedSide(final Dependency dependency) {
        return dependency.getName().equals("forge");
    }

    private boolean hasMatchingArtifact(ExternalModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getArtifacts().isEmpty()){
            return true;
        }

        return hasSourcesArtifact(externalModuleDependency);
    }

    private static boolean hasSourcesArtifact(ExternalModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getArtifacts().size() != 1) {
            return false;
        }

        final DependencyArtifact artifact = externalModuleDependency.getArtifacts().iterator().next();
        return artifact.getClassifier().equals("sources") && artifact.getExtension().equals("jar");
    }


    private static ForgeUserDevRuntimeDefinition buildForgeUserDevRuntimeFrom(Project project, Project configureProject, ExternalModuleDependency dependency) {
        final ForgeUserDevRuntimeExtension forgeRuntimeExtension = project.getExtensions().getByType(ForgeUserDevRuntimeExtension.class);

        return forgeRuntimeExtension.registerOrGet(builder -> {
            builder.configureFromProject(configureProject);
            builder.withForgeVersion(dependency.getVersion());

            final String version = dependency.getVersion() == null ? forgeRuntimeExtension.getDefaultVersion().get() : dependency.getVersion();
            builder.withName(String.format("dependencyForge%s", version == null ? "" : version));
        });
    }
}
