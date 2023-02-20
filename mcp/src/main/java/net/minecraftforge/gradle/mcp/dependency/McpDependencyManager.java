package net.minecraftforge.gradle.mcp.dependency;

import com.google.common.collect.Sets;
import net.minecraftforge.gradle.util.StringCapitalizationUtils;
import net.minecraftforge.gradle.common.util.CommonRuntimeTaskUtils;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.minecraftforge.gradle.common.runtime.tasks.AccessTransformer;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.dsl.common.extensions.AccessTransformers;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.base.util.DistributionType;
import net.minecraftforge.gradle.mcp.runtime.definition.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskProvider;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class McpDependencyManager {
    private static final McpDependencyManager INSTANCE = new McpDependencyManager();

    private McpDependencyManager() {
    }

    public static McpDependencyManager getInstance() {
        return INSTANCE;
    }

    public void apply(final Project project) {
        final DependencyReplacement dependencyReplacer = project.getExtensions().getByType(DependencyReplacement.class);

        dependencyReplacer.getReplacementHandlers().create("mcp", handler -> {
            handler.getReplacer().set(context -> {
                if (isNotAMatchingDependency(context.getDependency())) {
                    return Optional.empty();
                }

                if (!(context.getDependency() instanceof ExternalModuleDependency)) {
                    return Optional.empty();
                }

                final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) context.getDependency();

                final McpRuntimeDefinition runtimeDefinition = buildMcpRuntimeFromDependency(project, externalModuleDependency);
                return Optional.of(
                        new DependencyReplacementResult(
                                project,
                                name -> CommonRuntimeUtils.buildTaskName(runtimeDefinition, name),
                                runtimeDefinition.getSourceJarTask(),
                                runtimeDefinition.getRawJarTask(),
                                runtimeDefinition.getMinecraftDependenciesConfiguration(),
                                builder -> builder.setVersion(runtimeDefinition.getSpecification().getMcpVersion()),
                                runtimeDefinition::setReplacedDependency,
                                () -> Sets.newHashSet(runtimeDefinition.getAssetsTaskProvider(), runtimeDefinition.getNativesTaskProvider(), runtimeDefinition.getClientExtraJarProvider(), runtimeDefinition.getDebuggingMappingsTaskProvider())
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


    private static McpRuntimeDefinition buildMcpRuntimeFromDependency(Project project, ExternalModuleDependency dependency) {
        final McpRuntimeExtension runtimeExtension = project.getExtensions().getByType(McpRuntimeExtension.class);
        return runtimeExtension.maybeCreate(builder -> {
            builder.withDistributionType(DistributionType.valueOf(dependency.getName().replace("mcp_", "").toUpperCase(Locale.ROOT)));
            final String version = dependency.getVersion() == null ? runtimeExtension.getDefaultVersion().get() : dependency.getVersion();

            builder.withMcpVersion(version);
            builder.withName(String.format("dependencyMcp%s%s", StringCapitalizationUtils.capitalize(dependency.getName().replace("mcp_", "")), version == null ? "" : version));

            builder.withPreTaskAdapter("decompile", createAccessTransformerAdapter(project));
        });
    }

    private static TaskTreeAdapter createAccessTransformerAdapter(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);
        final AccessTransformers accessTransformerFiles = minecraftExtension.getAccessTransformers();

        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            if (accessTransformerFiles.getFiles().isEmpty() && accessTransformerFiles.getEntries().get().isEmpty()) {
                return null;
            }

            final TaskProvider<? extends AccessTransformer> accessTransformerTask = CommonRuntimeTaskUtils.createAccessTransformer(definition, "User", runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler, new ArrayList<>(accessTransformerFiles.getFiles().getFiles()), accessTransformerFiles.getEntries().get());
            accessTransformerTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            accessTransformerTask.configure(task -> task.dependsOn(previousTasksOutput));
            return accessTransformerTask;
        };
    }


}
