package net.neoforged.gradle.neoform.dependency;

import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.Context;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.extensions.RuntimesContainer;
import net.neoforged.gradle.dsl.common.runtime.spec.Specification;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormSdk;
import net.neoforged.gradle.neoform.runtime.NeoFormPublishingUtils;
import net.neoforged.gradle.neoform.runtime.NeoFormRuntimeBuilder;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementResult;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * This class installs a dependency replacement handler that replaces the following compileDependencies with the output
 * of a NeoForm runtime.
 * <p>
 * <ul>
 *     <li>net.minecraft:neoform_client</li>
 *     <li>net.minecraft:neoform_server</li>
 *     <li>net.minecraft:neoform_joined</li>
 * </ul>
 * <p>
 * The NeoForm version that should be used is determined from the version of the dependency.
 */
public final class NeoFormDependencyManager {
    private NeoFormDependencyManager() {
    }

    public static void apply(final Project project) {
        final DependencyReplacement dependencyReplacer = project.getExtensions().getByType(DependencyReplacement.class);

        dependencyReplacer.getReplacementHandlers().create("neoForm", handler -> {
            handler.getReplacer().set(NeoFormDependencyManager::replaceDependency);
        });
    }

    private static Optional<ReplacementResult> replaceDependency(Context context) {
        ModuleDependency dependency = context.getDependency();

        NeoFormTarget target = createTarget(dependency);
        if (target == null) {
            return Optional.empty();
        }

        if (target.version == null) {
            throw new IllegalStateException("Version is missing on NeoForm dependency " + dependency);
        }

        // Build the runtime used to produce the artifact
        Project project = context.getProject();
        RuntimesContainer container = project.getExtensions().getByType(RuntimesContainer.class);

        final NeoFormSdk sdk = NeoFormPublishingUtils.downloadAndParseSdkFile(project, target.version);

        final NeoFormRuntimeBuilder builder = new NeoFormRuntimeBuilder();

        final Definition runtimeDefinition = container.register(
                new Specification(
                        project,
                        "neoForm",
                        target.version(),
                        sdk.minecraftVersion(),
                        target.distribution()
                ),
                builder::build
        );

        return Optional.of(
                new ReplacementResult(
                        project,
                        runtimeDefinition.outputs().sources(),
                        runtimeDefinition.outputs().binaries(),
                        //TODO: Pass both configurations back
                        runtimeDefinition.dependencies().runtimeElements(),
                        Collections.emptySet()
                ));
    }

    @Nullable
    private static NeoFormTarget createTarget(ModuleDependency dependency) {

        if (!"net.minecraft".equals(dependency.getGroup())) {
            return null;
        }

        DistributionType distributionType;
        switch (dependency.getName()) {
            case "neoform_client" -> distributionType = DistributionType.CLIENT;
            case "neoform_server" -> distributionType = DistributionType.SERVER;
            case "neoform_joined" -> distributionType = DistributionType.JOINED;
            default -> {
                return null; // This dependency is not handled by this replacer
            }
        }

        if (!hasMatchingArtifact(dependency)) {
            return null;
        }

        return new NeoFormTarget(dependency.getVersion(), distributionType);
    }

    private static boolean hasMatchingArtifact(ModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getArtifacts().isEmpty()){
            return true;
        }

        return hasSourcesArtifact(externalModuleDependency);
    }

    private static boolean hasSourcesArtifact(ModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getArtifacts().size() != 1) {
            return false;
        }

        final DependencyArtifact artifact = externalModuleDependency.getArtifacts().iterator().next();
        return Objects.equals(artifact.getClassifier(), "sources") && Objects.equals(artifact.getExtension(), "jar");
    }

    private record NeoFormTarget(String version, DistributionType distribution) {}
    
}
