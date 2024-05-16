package net.neoforged.gradle.common.dependency;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.neoforged.gradle.common.runtime.tasks.GenerateExtraJar;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementResult;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class ExtraJarDependencyManager {

    private final Map<String, ReplacementResult> replacements = Maps.newHashMap();

    public static String generateClientCoordinateFor(final String version) {
        return "net.minecraft:client:" + version + ":client-extra";
    }
    
    public static String generateServerCoordinateFor(final String version) {
        return "net.minecraft:client:" + version + ":client-extra";
    }
    
    public static String generateCoordinateFor(final DistributionType type, final String version) {
        return String.format("net.minecraft:%s:%s:%s-extra", type.getName().toLowerCase(), version, type.getName().toLowerCase());
    }

    @Inject
    public ExtraJarDependencyManager(final Project project) {
        final DependencyReplacement dependencyReplacementsExtension = project.getExtensions().getByType(DependencyReplacement.class);

        dependencyReplacementsExtension.getReplacementHandlers().register("extraJar", dependencyReplacementHandler -> dependencyReplacementHandler.getReplacer().set(context -> {
                if (isNotAMatchingDependency(context.getDependency()))
                    return Optional.empty();

                return Optional.of(generateReplacement(project, context.getDependency()));
            }));
    }

    private boolean isNotAMatchingDependency(final Dependency dependencyToCheck) {
        if (dependencyToCheck instanceof ExternalModuleDependency) {
            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependencyToCheck;
            return externalModuleDependency.getGroup() == null || !externalModuleDependency.getGroup().equals("net.minecraft") || !isSupportedSide(dependencyToCheck) || !hasMatchingArtifact(externalModuleDependency);
        }

        return true;
    }

    private boolean isSupportedSide(final Dependency dependency) {
        return dependency.getName().equals("client") || dependency.getName().equals("server");
    }

    private boolean hasMatchingArtifact(ExternalModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getVersion() == null) {
            return false;
        }

        if (externalModuleDependency.getArtifacts().size() != 1) {
            return false;
        }

        final DependencyArtifact artifact = externalModuleDependency.getArtifacts().iterator().next();
        return Objects.equals(artifact.getClassifier(), "client-extra") || Objects.equals(artifact.getClassifier(), "server-extra");
    }

    private ReplacementResult generateReplacement(final Project project, final Dependency dependency) {
        final String minecraftVersion = dependency.getVersion();
        return replacements.computeIfAbsent(minecraftVersion, (v) -> {
            final MinecraftArtifactCache minecraftArtifactCacheExtension = project.getExtensions().getByType(MinecraftArtifactCache.class);

            final TaskProvider<GenerateExtraJar> extraJarTaskProvider = project.getTasks().register("create" + minecraftVersion + StringUtils.capitalize(dependency.getName()) + "ExtraJar", GenerateExtraJar.class, task -> {
                task.getOriginalJar().set(minecraftArtifactCacheExtension.cacheVersionArtifact(minecraftVersion, DistributionType.CLIENT));
                task.getOutput().set(project.getLayout().getBuildDirectory().dir("jars/extra/" + dependency.getName()).map(cacheDir -> cacheDir.dir(Objects.requireNonNull(minecraftVersion)).file( dependency.getName() + "-extra.jar")));
            });

            return new ReplacementResult(
                    project,
                    extraJarTaskProvider,
                    project.getConfigurations().detachedConfiguration(),
                    Collections.emptySet()
            );
        });
    }
}
