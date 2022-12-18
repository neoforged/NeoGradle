package net.minecraftforge.gradle.mcp.dependency;

import net.minecraftforge.gradle.common.extensions.base.BaseFilesWithEntriesExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementExtension;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.common.runtime.spec.TaskTreeAdapter;
import net.minecraftforge.gradle.common.runtime.tasks.AccessTransformer;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.ArtifactSide;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.mcp.runtime.spec.builder.McpRuntimeSpecBuilder;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskProvider;

import java.util.*;

public final class McpDependencyManager {
    private static final McpDependencyManager INSTANCE = new McpDependencyManager();

    private McpDependencyManager() {
    }

    public static McpDependencyManager getInstance() {
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

            final McpRuntimeDefinition runtimeDefinition = buildMcpRuntimeFromDependency(project, context.project(), externalModuleDependency);
            return Optional.of(
                    new DependencyReplacementResult(
                            project,
                            name -> CommonRuntimeUtils.buildTaskName(runtimeDefinition, name),
                            runtimeDefinition.sourceJarTask(),
                            runtimeDefinition.rawJarTask(),
                            runtimeDefinition.minecraftDependenciesConfiguration(),
                            builder -> builder.withVersion(runtimeDefinition.spec().mcpVersion()),
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
        return Objects.equals(artifact.getClassifier(), "sources") && Objects.equals(artifact.getExtension(), "jar");
    }


    private static McpRuntimeDefinition buildMcpRuntimeFromDependency(Project project, Project configureProject, ExternalModuleDependency dependency) {
        final McpRuntimeExtension runtimeExtension = project.getExtensions().getByType(McpRuntimeExtension.class);
        return runtimeExtension.maybeCreate((Action<McpRuntimeSpecBuilder>) builder -> {
            builder.configureFromProject(configureProject);
            builder.withSide(ArtifactSide.valueOf(dependency.getName().replace("mcp_", "").toUpperCase(Locale.ROOT)));
            final String version = dependency.getVersion() == null ? runtimeExtension.getDefaultVersion().get() : dependency.getVersion();

            builder.withMcpVersion(version);
            builder.withName(String.format("dependencyMcp%s%s", Utils.capitalize(dependency.getName().replace("mcp_", "")), version == null ? "" : version));

            builder.withPreTaskAdapter("decompile", createAccessTransformerAdapter(project));
        });
    }

    private static TaskTreeAdapter createAccessTransformerAdapter(final Project project) {
        final MinecraftExtension minecraftExtension = project.getExtensions().getByType(MinecraftExtension.class);
        final BaseFilesWithEntriesExtension accessTransformerFiles = minecraftExtension.getAccessTransformers();

        return (spec, previousTasksOutput, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends AccessTransformer> accessTransformerTask = McpRuntimeUtils.createAccessTransformer(spec, "User", new ArrayList<>(accessTransformerFiles.getFiles().getFiles()), accessTransformerFiles.getEntries().get());
            accessTransformerTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(ITaskWithOutput::getOutput)));
            accessTransformerTask.configure(task -> task.dependsOn(previousTasksOutput));
            return accessTransformerTask;
        };
    }


}
