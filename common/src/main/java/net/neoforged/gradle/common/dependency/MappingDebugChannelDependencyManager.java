package net.neoforged.gradle.common.dependency;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.extensions.RuntimesExtension;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.neoforged.gradle.dsl.common.runtime.naming.GenerationTaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class MappingDebugChannelDependencyManager {

    private final Map<String, DependencyReplacementResult> replacements = Maps.newHashMap();
    private final Project project;

    public static String generateCoordinateFor(
            final NamingChannel channel,
            final Map<String, String> version,
            final CommonRuntimeDefinition<?> runtimeDefinition
    ) {
        final String encodedVersion = channel.getDependencyNotationVersionManager().get().encode(version) + "¿" + runtimeDefinition.getSpecification().getVersionedName();
        return "fg.mappings:" + channel.getName() + ":" + encodedVersion + ":debug-mappings";
    }

    @Inject
    public MappingDebugChannelDependencyManager(final Project project) {
        this.project = project;

        final DependencyReplacement dependencyReplacementsExtension = project.getExtensions().getByType(DependencyReplacement.class);

        dependencyReplacementsExtension.getReplacementHandlers().register("mappingDebugJar", dependencyReplacementHandler -> dependencyReplacementHandler.getReplacer().set(context -> {
            if (isNotAMatchingDependency(context.getDependency()))
                return Optional.empty();

            return Optional.of(generateReplacement(project, context.getDependency()));
        }));
    }

    private boolean isNotAMatchingDependency(final Dependency dependencyToCheck) {
        if (dependencyToCheck instanceof ExternalModuleDependency) {
            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependencyToCheck;
            return externalModuleDependency.getGroup() == null || !externalModuleDependency.getGroup().equals("fg.mappings") || !isSupportedSide(dependencyToCheck) || !hasMatchingArtifact(externalModuleDependency);
        }

        return true;
    }

    private boolean isSupportedSide(final Dependency dependency) {
        final Minecraft minecraft = project.getExtensions().getByType(Minecraft.class);

        return minecraft.getNamingChannels().getNames().contains(dependency.getName());
    }

    private boolean hasMatchingArtifact(ExternalModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getVersion() == null) {
            return false;
        }

        if (externalModuleDependency.getArtifacts().size() != 1) {
            return false;
        }

        final DependencyArtifact artifact = externalModuleDependency.getArtifacts().iterator().next();
        return Objects.equals(artifact.getClassifier(), "debug-mappings");
    }

    private DependencyReplacementResult generateReplacement(final Project project, final Dependency dependency) {
        final String encodedVersion = Objects.requireNonNull(dependency.getVersion()).substring(0, dependency.getVersion().lastIndexOf("¿"));
        final String namingChannelName = dependency.getName();
        final String runtimeName = dependency.getVersion().substring(dependency.getVersion().lastIndexOf("¿") + 1);

        final Minecraft minecraft = project.getExtensions().getByType(Minecraft.class);
        final NamingChannel namingChannel = minecraft.getNamingChannels().getByName(namingChannelName);
        final CommonRuntimeDefinition<?> runtimeDefinition = project.getExtensions().getByType(RuntimesExtension.class).findDefinitionByNameOrIdentifier(runtimeName);
        if (runtimeDefinition == null) {
            throw new IllegalArgumentException("Could not find runtime with name: " + runtimeName + " for dependency: " + dependency);
        }

        return replacements.computeIfAbsent(String.format("%s-%s-%s", namingChannelName, encodedVersion, runtimeName), (v) -> {
            final String environmentName = String.format("debuggingMappingsZipDependency%s", v);

            final TaskProvider<? extends Runtime> generateDebugMappingsJar =
                    namingChannel.getGenerateDebuggingMappingsJarTaskBuilder().get()
                            .build(new GenerationTaskBuildingContext(
                                    project,
                                    environmentName,
                                    taskName -> CommonRuntimeUtils.buildTaskName(taskName, environmentName),
                                    CommonRuntimeExtension.buildDefaultArtifactProviderTasks(runtimeDefinition.getSpecification()),
                                    runtimeDefinition
                            ));

            runtimeDefinition.configureAssociatedTask(generateDebugMappingsJar);

            return new DependencyReplacementResult(
                    project,
                    name -> name,
                    generateDebugMappingsJar,
                    generateDebugMappingsJar,
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
