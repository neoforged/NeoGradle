package net.minecraftforge.gradle.mcp.dependency;

import com.google.common.collect.ImmutableMap;
import net.minecraftforge.gradle.common.ide.IdeManager;
import net.minecraftforge.gradle.common.repository.IvyDummyRepositoryEntry;
import net.minecraftforge.gradle.common.extensions.IvyDummyRepositoryExtension;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import net.minecraftforge.gradle.mcp.runtime.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.mcp.runtime.tasks.RawAndSourceCombiner;
import net.minecraftforge.gradle.mcp.runtime.tasks.McpRuntime;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Path;

public final class McpRuntimeBasedMinecraftDependencyManager {
    private static final McpRuntimeBasedMinecraftDependencyManager INSTANCE = new McpRuntimeBasedMinecraftDependencyManager();

    private McpRuntimeBasedMinecraftDependencyManager() {
    }

    public static McpRuntimeBasedMinecraftDependencyManager getInstance() {
        return INSTANCE;
    }

    public void apply(final Project project) {
        project.getConfigurations().configureEach(configuration -> {
            configuration.getDependencies().whenObjectAdded(addedDependency -> {
                if (isMatchingDependency(addedDependency)) {
                    configuration.getDependencies().remove(addedDependency);
                    configuration.getDependencies().add(createReplacementDependency(project, addedDependency));
                }
            });
        });
    }

    private boolean isMatchingDependency(final Dependency dependencyToCheck) {
        if (dependencyToCheck instanceof ExternalModuleDependency externalModuleDependency) {
            return externalModuleDependency.getGroup() != null && externalModuleDependency.getGroup().equals("net.minecraft") && isSupportedSide(dependencyToCheck) && hasMatchingArtifact(externalModuleDependency);
        }

        return false;
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

    private Dependency createReplacementDependency(final Project project, final Dependency dependency) {
        if (!isMatchingDependency(dependency) || !(dependency instanceof ExternalModuleDependency externalModuleDependency)) {
            throw new IllegalArgumentException("Dependency is not a supported Minecraft dependency");
        }

        if (IdeManager.getInstance().isIdeImportInProgress()) {
            return createCombinedRawJarReplacementDependency(project, externalModuleDependency);
        }

        if (!hasSourcesArtifact(externalModuleDependency)) {
            return createNoneSourceJarReplacementDependency(project, externalModuleDependency);
        }

        return createSourceJarReplacementDependency(project, externalModuleDependency);
    }

    private Dependency createNoneSourceJarReplacementDependency(final Project project, final ExternalModuleDependency dependency) {
        final McpRuntimeDefinition runtimeDefinition = buildMcpRuntimeFromDependency(project, project, dependency);
        final Configuration configuration = project.getConfigurations().maybeCreate("mcpRuntime%s%s".formatted(Utils.capitalize(runtimeDefinition.spec().name()), runtimeDefinition.spec().mcpVersion()));
        configuration.setCanBeResolved(true);
        configuration.setCanBeConsumed(true);

        final TaskProvider<? extends McpRuntime> outputProvider = runtimeDefinition.rawJarTask();

        final String artifactSelectionTaskName = "selectRawArtifactFrom%s".formatted(Utils.capitalize(runtimeDefinition.spec().name()));
        final TaskProvider<? extends ArtifactFromOutput> rawArtifactSelectionTask = project.getTasks().register(artifactSelectionTaskName, ArtifactFromOutput.class, artifactFromOutput -> {
            artifactFromOutput.setGroup("mcp");
            artifactFromOutput.setDescription("Selects the raw artifact from the %s runtime".formatted(runtimeDefinition.spec().name()));

            artifactFromOutput.getInput().set(outputProvider.flatMap(McpRuntime::getOutput));
            artifactFromOutput.getOutput().set(project.getLayout().getBuildDirectory().file("artifacts/raw/%s-%s.jar".formatted(runtimeDefinition.spec().name(), runtimeDefinition.spec().mcpVersion())));
        });

        //IdeManager.getInstance().registerTaskToRun(project, rawArtifactSelectionTask);

        return configureAndConvertConfigurationToDependency(project, configuration, rawArtifactSelectionTask);
    }

    private Dependency createSourceJarReplacementDependency(final Project project, final ExternalModuleDependency dependency) {
        final McpRuntimeDefinition runtimeDefinition = buildMcpRuntimeFromDependency(project, project, dependency);
        final Configuration configuration = project.getConfigurations().maybeCreate("mcpRuntimeForSource%s%s".formatted(Utils.capitalize(runtimeDefinition.spec().name()), runtimeDefinition.spec().mcpVersion()));
        configuration.setCanBeResolved(true);
        configuration.setCanBeConsumed(true);

        final TaskProvider<? extends McpRuntime> outputProvider = runtimeDefinition.lastTask();

        final String artifactSelectionTaskName = "selectSourcesArtifactFrom%s".formatted(Utils.capitalize(runtimeDefinition.spec().name()));
        final TaskProvider<? extends ArtifactFromOutput> sourcesArtifactSelectionTask = project.getTasks().register(artifactSelectionTaskName, ArtifactFromOutput.class, artifactFromOutput -> {
            artifactFromOutput.setGroup("mcp");
            artifactFromOutput.setDescription("Selects the sources artifact from the output of the %s runtime.".formatted(runtimeDefinition.spec().name()));

            artifactFromOutput.getInput().set(outputProvider.flatMap(McpRuntime::getOutput));
            artifactFromOutput.getOutput().set(project.getLayout().getBuildDirectory().file("artifacts/sources/%s-%s-sources.jar".formatted(runtimeDefinition.spec().name(), runtimeDefinition.spec().mcpVersion())));
        });

        return configureAndConvertConfigurationToDependency(project, configuration, sourcesArtifactSelectionTask);
    }

    private Dependency createCombinedRawJarReplacementDependency(final Project project, final ExternalModuleDependency dependency) throws RuntimeException {
        final McpRuntimeDefinition runtimeDefinition = buildMcpRuntimeFromDependency(project, project, dependency);
        final Configuration configuration = project.getConfigurations().maybeCreate("mcpRuntimeCombined%s%s".formatted(Utils.capitalize(runtimeDefinition.spec().name()), runtimeDefinition.spec().mcpVersion()));
        configuration.setCanBeResolved(true);
        configuration.setCanBeConsumed(true);

        final TaskProvider<? extends McpRuntime> rawProvider = runtimeDefinition.rawJarTask();
        final TaskProvider<? extends McpRuntime> sourcesProvider = runtimeDefinition.lastTask();

        final IvyDummyRepositoryExtension extension = project.getExtensions().getByType(IvyDummyRepositoryExtension.class);
        final Provider<Directory> repoBaseDir = extension.createRepoBaseDir();
        final IvyDummyRepositoryEntry entry;
        try {
            entry = extension.withDependency(builder -> builder.with(dependency).withVersion(runtimeDefinition.spec().mcpVersion()));
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException("Failed to create the dummy dependency for: %s".formatted(dependency.toString()), e);
        }

        final String dependencyExporterTaskName = "combined%s".formatted(Utils.capitalize(runtimeDefinition.spec().name()));
        final TaskProvider<? extends RawAndSourceCombiner> rawAndSourceCombinerTask = project.getTasks().register(dependencyExporterTaskName, RawAndSourceCombiner.class, rawAndSourceCombiner -> {
            rawAndSourceCombiner.setGroup("mcp");
            rawAndSourceCombiner.setDescription("Combines the raw and sources jars into a single task execution tree for: %s with version: %s".formatted(runtimeDefinition.spec().name(), runtimeDefinition.spec().mcpVersion()));

            rawAndSourceCombiner.getRawJarInput().set(rawProvider.flatMap(McpRuntime::getOutput));
            rawAndSourceCombiner.getSourceJarInput().set(sourcesProvider.flatMap(McpRuntime::getOutput));

            rawAndSourceCombiner.getRawJarOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.artifactPath(dir.getAsFile().toPath()).toFile())));
            rawAndSourceCombiner.getSourceJarOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.asSources().artifactPath(dir.getAsFile().toPath()).toFile())));
        });

        IdeManager.getInstance().registerTaskToRun(project, rawAndSourceCombinerTask);

        return entry.asDependency(project);
    }

    @NotNull
    private static Dependency configureAndConvertConfigurationToDependency(Project project, Configuration configuration, TaskProvider<?> outputProvider) {
        configuration.getOutgoing().artifact(outputProvider, artifact -> {
            artifact.builtBy(outputProvider);
            artifact.setName("minecraft");
            artifact.setType("jar");
        });

        return project.getDependencies().create(project.getDependencies().project(
                ImmutableMap.of(
                        "path", project.getPath(),
                        "configuration", configuration.getName()
                )
        ));
    }

    private static McpRuntimeDefinition buildMcpRuntimeFromDependency(Project project, Project configureProject, ExternalModuleDependency dependency) {
        final McpRuntimeExtension runtimeExtension = project.getExtensions().getByType(McpRuntimeExtension.class);
        return runtimeExtension.registerOrGet(builder -> {
            builder.configureFromProject(configureProject);
            builder.withSide(dependency.getName().replace("mcp_", ""));
            builder.withMcpVersion(dependency.getVersion());
            builder.withName("dependency%s".formatted(Utils.capitalize(dependency.getName().replace("mcp_", ""))));
        });
    }


}
