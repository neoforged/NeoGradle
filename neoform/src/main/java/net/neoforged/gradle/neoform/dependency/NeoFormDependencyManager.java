package net.neoforged.gradle.neoform.dependency;

import com.google.common.collect.Sets;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.Context;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * This class installs a dependency replacement handler that replaces the following dependencies with the output
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

    private static Optional<DependencyReplacementResult> replaceDependency(Context context) {
        ModuleDependency dependency = context.getDependency();

        NeoFormTarget target = getNeoFormTargetFromDependency(dependency);
        if (target == null) {
            return Optional.empty();
        }

        if (target.version == null) {
            throw new IllegalStateException("Version is missing on NeoForm dependency " + dependency);
        }

        // Build the runtime used to produce the artifact
        Project project = context.getProject();
        NeoFormRuntimeExtension runtimeExtension = project.getExtensions().getByType(NeoFormRuntimeExtension.class);
        NeoFormRuntimeDefinition runtime = runtimeExtension.maybeCreate(builder -> {
            builder.withDistributionType(target.distribution).withNeoFormVersion(target.version);
            NeoFormRuntimeUtils.configureDefaultRuntimeSpecBuilder(project, builder);
        });

        return Optional.of(
                new DependencyReplacementResult(
                        project,
                        Optional.of(ConfigurationUtils.findReplacementConfigurations(project, context.getConfiguration())),
                        name -> CommonRuntimeUtils.buildTaskName(runtime, name),
                        runtime.getSourceJarTask(),
                        runtime.getRawJarTask(),
                        runtime.getMinecraftDependenciesConfiguration(),
                        builder -> builder.setVersion(runtime.getSpecification().getNeoFormVersion()),
                        builder -> builder.setVersion(runtime.getSpecification().getNeoFormVersion()),
                        runtime::setReplacedDependency,
                        runtime::onRepoWritten,
                        Sets::newHashSet
                ));
    }

    @Nullable
    private static NeoFormTarget getNeoFormTargetFromDependency(ModuleDependency dependency) {

        if (!"net.minecraft".equals(dependency.getGroup())) {
            return null;
        }

        DistributionType distributionType;
        switch (dependency.getName()) {
            case "neoform_client":
                distributionType = DistributionType.CLIENT;
                break;
            case "neoform_server":
                distributionType = DistributionType.SERVER;
                break;
            case "neoform_joined":
                distributionType = DistributionType.JOINED;
                break;
            default:
                return null; // This dependency is not handled by this replacer
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

    private static final class NeoFormTarget {
        private final String version;
        private final DistributionType distribution;

        public NeoFormTarget(String version, DistributionType distribution) {
            this.version = version;
            this.distribution = distribution;
        }
    }
    
}
