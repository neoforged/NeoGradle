package net.neoforged.gradle.neoform.dependency;

import com.google.common.collect.Sets;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.neoform.util.NeoFormAccessTransformerUtils;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.provider.Provider;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class NeoFormDependencyManager {
    private static final NeoFormDependencyManager INSTANCE = new NeoFormDependencyManager();

    private NeoFormDependencyManager() {
    }

    public static NeoFormDependencyManager getInstance() {
        return INSTANCE;
    }

    public void apply(final Project project) {
        final DependencyReplacement dependencyReplacer = project.getExtensions().getByType(DependencyReplacement.class);

        dependencyReplacer.getReplacementHandlers().create("neoForm", handler -> {
            handler.getReplacer().set(context -> {
                if (isNotAMatchingDependency(context.getDependency())) {
                    return Optional.empty();
                }

                if (!(context.getDependency() instanceof ExternalModuleDependency)) {
                    return Optional.empty();
                }

                final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) context.getDependency();

                final NeoFormRuntimeDefinition runtimeDefinition = buildNeoFormRuntimeFromDependency(project, externalModuleDependency);
                return Optional.of(
                        new DependencyReplacementResult(
                                project,
                                name -> CommonRuntimeUtils.buildTaskName(runtimeDefinition, name),
                                runtimeDefinition.getSourceJarTask(),
                                runtimeDefinition.getRawJarTask(),
                                runtimeDefinition.getMinecraftDependenciesConfiguration(),
                                builder -> builder.setVersion(runtimeDefinition.getSpecification().getNeoFormArtifact().getVersion()),
                                builder -> builder.setVersion(runtimeDefinition.getSpecification().getNeoFormArtifact().getVersion()),
                                runtimeDefinition::setReplacedDependency,
                                runtimeDefinition::onRepoWritten,
                                () -> Sets.newHashSet(runtimeDefinition.getAssetsTaskProvider(), runtimeDefinition.getNativesTaskProvider(), runtimeDefinition.getDebuggingMappingsTaskProvider())
                ));
            });
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
        return dependency.getName().equalsIgnoreCase("neoform_client") ||
                dependency.getName().equalsIgnoreCase("neoform_server") ||
                dependency.getName().equalsIgnoreCase("neoform_joined");
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
        return Objects.equals(artifact.getClassifier(), "sources") && Objects.equals(artifact.getExtension(), "jar");
    }


    private static NeoFormRuntimeDefinition buildNeoFormRuntimeFromDependency(Project project, ExternalModuleDependency dependency) {
        final NeoFormRuntimeExtension runtimeExtension = project.getExtensions().getByType(NeoFormRuntimeExtension.class);
        return runtimeExtension.maybeCreate(builder -> {
            builder.withDistributionType(DistributionType.valueOf(dependency.getName().toLowerCase().replace("neoform_", "").toUpperCase(Locale.ROOT)));
            if (dependency.getVersion() == null) {
                throw new IllegalStateException("Version is not defined on NeoForm dependency");
            }

            builder.withNeoFormVersion(dependency.getVersion());
            builder.withPreTaskAdapter("decompile", NeoFormAccessTransformerUtils.createAccessTransformerAdapter(project));
        });
    }
}
