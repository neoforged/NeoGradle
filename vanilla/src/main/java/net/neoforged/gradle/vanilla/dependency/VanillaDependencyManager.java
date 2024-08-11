package net.neoforged.gradle.vanilla.dependency;

import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementResult;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.neoforged.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;

import java.util.Collections;
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

                    final VanillaRuntimeDefinition runtimeDefinition = buildVanillaRuntimeDefinition(project, externalModuleDependency);
                    return Optional.of(
                            new ReplacementResult(
                                    project,
                                    runtimeDefinition.getSourceJarTask(),
                                    runtimeDefinition.getRawJarTask(),
                                    project.getConfigurations().detachedConfiguration(),
                                    runtimeDefinition.getMinecraftDependenciesConfiguration(),
                                    Collections.emptySet()
                            ));
                });
            }
        });
    }

    private boolean isNotAMatchingDependency(final Dependency dependencyToCheck) {
        if (dependencyToCheck instanceof ExternalModuleDependency) {
            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependencyToCheck;
            return !"net.minecraft".equals(externalModuleDependency.getGroup()) || !isSupportedSide(dependencyToCheck) || !hasMatchingArtifact(externalModuleDependency);
        }

        return true;
    }

    private boolean isSupportedSide(final Dependency dependency) {
        return dependency.getName().equals("client") ||
                dependency.getName().equals("server");
    }

    private boolean hasMatchingArtifact(ExternalModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getArtifacts().isEmpty()) {
            return true;
        }

        return hasSourcesArtifact(externalModuleDependency);
    }

    private static boolean hasSourcesArtifact(ExternalModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getArtifacts().size() != 1) {
            return false;
        }

        final DependencyArtifact artifact = externalModuleDependency.getArtifacts().iterator().next();
        return "sources".equals(artifact.getClassifier()) && "jar".equals(artifact.getExtension());
    }


    private static VanillaRuntimeDefinition buildVanillaRuntimeDefinition(Project project, ExternalModuleDependency dependency) {
        final VanillaRuntimeExtension runtimeExtension = project.getExtensions().getByType(VanillaRuntimeExtension.class);

        return runtimeExtension.maybeCreateFor(dependency, builder -> {
            final String version = dependency.getVersion() == null ? runtimeExtension.getVersion().get() : dependency.getVersion();

            builder.withMinecraftArtifact(StringCapitalizationUtils.deCapitalize(dependency.getName()));
            builder.withDistributionType(DistributionType.valueOf(dependency.getName().toUpperCase(Locale.ROOT)));
            builder.withMinecraftVersion(version);
            builder.withFartVersion(runtimeExtension.getFartVersion());
            builder.withForgeFlowerVersion(runtimeExtension.getVineFlowerVersion());
            builder.withAccessTransformerApplierVersion(runtimeExtension.getAccessTransformerApplierVersion());
        });
    }


}
