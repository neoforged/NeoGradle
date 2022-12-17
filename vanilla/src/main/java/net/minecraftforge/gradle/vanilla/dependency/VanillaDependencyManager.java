package net.minecraftforge.gradle.vanilla.dependency;

import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementExtension;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.common.util.ArtifactSide;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.minecraftforge.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import net.minecraftforge.gradle.vanilla.runtime.spec.builder.VanillaRuntimeSpecBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public final class VanillaDependencyManager {
    private static final VanillaDependencyManager INSTANCE = new VanillaDependencyManager();

    private VanillaDependencyManager() {
    }

    public static VanillaDependencyManager getInstance() {
        return INSTANCE;
    }

    public void apply(final Project project) {
        final DependencyReplacementExtension dependencyReplacer = project.getExtensions().getByType(DependencyReplacementExtension.class);
        dependencyReplacer.getReplacementHandlers().add(context -> {
            if (isNotAMatchingDependency(context.dependency())) {
                return Optional.empty();
            }

            if (!(context.dependency() instanceof ExternalModuleDependency)) {
                return Optional.empty();
            }

            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) context.dependency();

            final VanillaRuntimeDefinition runtimeDefinition = buildVanillaRuntimeDefinition(project, context.project(), externalModuleDependency);
            return Optional.of(
                    new DependencyReplacementResult(
                            project,
                            name -> CommonRuntimeUtils.buildTaskName(runtimeDefinition, name),
                            runtimeDefinition.sourceJarTask(),
                            runtimeDefinition.rawJarTask(),
                            runtimeDefinition.minecraftDependenciesConfiguration(),
                            builder -> builder.withVersion(runtimeDefinition.spec().minecraftVersion()),
                            runtimeDefinition::replacedDependency)
            );
        });
    }

    private boolean isNotAMatchingDependency(final Dependency dependencyToCheck) {
        if (dependencyToCheck instanceof ExternalModuleDependency) {
            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependencyToCheck;
            return externalModuleDependency.getGroup() == null || !externalModuleDependency.getGroup().equals("net.minecraft") || !isSupportedSide(dependencyToCheck) || !hasMatchingArtifact(externalModuleDependency);
        }

        return true;
    }

    private boolean isSupportedSide(final Dependency dependency) {
        return dependency.getName().equals("mcp_client") ||
                dependency.getName().equals("mcp_server") ||
                dependency.getName().equals("mcp_joined");
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


    private static VanillaRuntimeDefinition buildVanillaRuntimeDefinition(Project project, Project configureProject, ExternalModuleDependency dependency) {
        final VanillaRuntimeExtension runtimeExtension = project.getExtensions().getByType(VanillaRuntimeExtension.class);

        return runtimeExtension.maybeCreate((Consumer<VanillaRuntimeSpecBuilder>) builder -> {
            final String version = dependency.getVersion() == null ? runtimeExtension.getVersion().get() : dependency.getVersion();

            builder.configureFromProject(configureProject);
            builder.withName(dependency.getName());
            builder.withSide(ArtifactSide.valueOf(dependency.getName().replace("mcp_", "").toUpperCase(Locale.ROOT)));
            builder.withMinecraftVersion(version);
            builder.withFartVersion(runtimeExtension.getFartVersion());
            builder.withForgeFlowerVersion(runtimeExtension.getForgeFlowerVersion());
            builder.withAccessTransformerApplierVersion(runtimeExtension.getAccessTransformerApplierVersion());

            builder.withName(String.format("dependencyVanilla%s%s", Utils.capitalize(dependency.getName().replace("mcp_", "")), version == null ? "" : version));
        });
    }


}
