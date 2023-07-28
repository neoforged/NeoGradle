package net.neoforged.gradle.common.dependency;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.neoforged.gradle.common.runtime.tasks.ClientExtraJar;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class ClientExtraJarDependencyManager {

    private final Map<String, DependencyReplacementResult> replacements = Maps.newHashMap();

    public static String generateCoordinateFor(final String version) {
        return "net.minecraft:client:" + version + ":client-extra";
    }

    @Inject
    public ClientExtraJarDependencyManager(final Project project) {
        final DependencyReplacement dependencyReplacementsExtension = project.getExtensions().getByType(DependencyReplacement.class);

        dependencyReplacementsExtension.getReplacementHandlers().register("clientExtraJar", dependencyReplacementHandler -> dependencyReplacementHandler.getReplacer().set(context -> {
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
        return dependency.getName().equals("client");
    }

    private boolean hasMatchingArtifact(ExternalModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getVersion() == null) {
            return false;
        }

        if (externalModuleDependency.getArtifacts().size() != 1) {
            return false;
        }

        final DependencyArtifact artifact = externalModuleDependency.getArtifacts().iterator().next();
        return Objects.equals(artifact.getClassifier(), "client-extra");
    }

    private DependencyReplacementResult generateReplacement(final Project project, final Dependency dependency) {
        final String minecraftVersion = dependency.getVersion();
        return replacements.computeIfAbsent(minecraftVersion, (v) -> {
            final MinecraftArtifactCache minecraftArtifactCacheExtension = project.getExtensions().getByType(MinecraftArtifactCache.class);

            final TaskProvider<ClientExtraJar> extraJarTaskProvider = project.getTasks().register("create" + minecraftVersion + "clientEntryJar", ClientExtraJar.class, task -> {
                task.getOriginalClientJar().set(minecraftArtifactCacheExtension.cacheVersionArtifact(minecraftVersion, DistributionType.CLIENT));
                task.getOutput().set(project.getLayout().getBuildDirectory().dir("clientExtraJar").map(cejDir -> cejDir.dir(Objects.requireNonNull(minecraftVersion)).file("client-extra.jar")));
            });

            return new DependencyReplacementResult(
                    project,
                    name -> name,
                    extraJarTaskProvider,
                    extraJarTaskProvider,
                    project.getConfigurations().detachedConfiguration(),
                    builder -> { },
                    builder -> { },
                    (dep) -> { },
                    (task) -> { },
                    Sets::newHashSet
            );
        });
    }
}
