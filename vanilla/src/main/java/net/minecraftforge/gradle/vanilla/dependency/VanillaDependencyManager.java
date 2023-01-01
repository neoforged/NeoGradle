package net.minecraftforge.gradle.vanilla.dependency;

import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.minecraftforge.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;

import java.util.Locale;
import java.util.Optional;

public final class VanillaDependencyManager {
    private static final VanillaDependencyManager INSTANCE = new VanillaDependencyManager();

    private VanillaDependencyManager() {
    }

    public static VanillaDependencyManager getInstance() {
        return INSTANCE;
    }

    public void apply(final Project project) {
        final DependencyReplacement dependencyReplacer = project.getExtensions().getByType(DependencyReplacement.class);

        dependencyReplacer.getReplacementHandlers().create("vanilla", new Action<DependencyReplacementHandler>() {
            @Override
            public void execute(DependencyReplacementHandler dependencyReplacementHandler) {
                dependencyReplacementHandler.getReplacer().set(context -> {
                    if (isNotAMatchingDependency(context.getDependency())) {
                        return Optional.empty();
                    }

                    if (!(context.getDependency() instanceof ExternalModuleDependency)) {
                        return Optional.empty();
                    }

                    final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) context.getDependency();

                    final VanillaRuntimeDefinition runtimeDefinition = buildVanillaRuntimeDefinition(project, context.getProject(), externalModuleDependency);
                    return Optional.of(
                            new DependencyReplacementResult(
                                    project,
                                    name -> CommonRuntimeUtils.buildTaskName(runtimeDefinition, name),
                                    runtimeDefinition.getSourceJarTask(),
                                    runtimeDefinition.getRawJarTask(),
                                    runtimeDefinition.getMinecraftDependenciesConfiguration(),
                                    builder -> builder.setVersion(runtimeDefinition.getSpecification().getMinecraftVersion()),
                                    runtimeDefinition::setReplacedDependency)
                    );
                });
            }
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

        return runtimeExtension.maybeCreate(builder -> {
            final String version = dependency.getVersion() == null ? runtimeExtension.getVersion().get() : dependency.getVersion();

            builder.withName(dependency.getName());
            builder.withDistributionType(DistributionType.valueOf(dependency.getName().replace("mcp_", "").toUpperCase(Locale.ROOT)));
            builder.withMinecraftVersion(version);
            builder.withFartVersion(runtimeExtension.getFartVersion());
            builder.withForgeFlowerVersion(runtimeExtension.getForgeFlowerVersion());
            builder.withAccessTransformerApplierVersion(runtimeExtension.getAccessTransformerApplierVersion());

            builder.withName(String.format("dependencyVanilla%s%s", Utils.capitalize(dependency.getName().replace("mcp_", "")), version == null ? "" : version));
        });
    }


}
