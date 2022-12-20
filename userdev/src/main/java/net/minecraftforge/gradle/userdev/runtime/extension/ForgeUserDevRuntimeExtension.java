package net.minecraftforge.gradle.userdev.runtime.extension;

import com.google.common.collect.Maps;
import groovy.lang.GroovyObjectSupport;
import net.minecraftforge.gradle.common.runtime.spec.TaskTreeAdapter;
import net.minecraftforge.gradle.common.runtime.tasks.AccessTransformer;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.common.util.*;
import net.minecraftforge.gradle.configurations.UserDevConfigurationSpecV2;
import net.minecraftforge.gradle.dsl.common.util.ArtifactSide;
import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import net.minecraftforge.gradle.mcp.runtime.spec.builder.McpRuntimeSpecBuilder;
import net.minecraftforge.gradle.mcp.runtime.tasks.*;
import net.minecraftforge.gradle.mcp.util.McpRuntimeUtils;
import net.minecraftforge.gradle.userdev.extension.ForgeUserDevExtension;
import net.minecraftforge.gradle.userdev.runtime.ForgeUserDevRuntimeDefinition;
import net.minecraftforge.gradle.userdev.runtime.spec.ForgeUserDevRuntimeSpec;
import net.minecraftforge.gradle.userdev.runtime.spec.builder.ForgeUserDevRuntimeSpecBuilder;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"}) //API Design
public abstract class ForgeUserDevRuntimeExtension extends GroovyObjectSupport implements IConfigurableObject<ForgeUserDevRuntimeExtension> {

    private final Project project;
    private final Map<String, ForgeUserDevRuntimeDefinition> runtimes = Maps.newHashMap();

    @javax.inject.Inject
    public ForgeUserDevRuntimeExtension(Project project) {
        this.project = project;

        this.getDefaultVersion().convention(
                project.getExtensions().getByType(ForgeUserDevExtension.class).getDefaultVersion()
        );
    }

    public Project getProject() {
        return project;
    }

    public abstract Property<String> getDefaultVersion();

    public ForgeUserDevRuntimeDefinition registerOrGet(Action<ForgeUserDevRuntimeSpecBuilder> builder) {
        final ForgeUserDevRuntimeSpec runtimeSpec = createSpec(builder);

        if (runtimes.containsKey(runtimeSpec.name())) {
            final ForgeUserDevRuntimeDefinition runtimeDefinition = runtimes.get(runtimeSpec.name());
            if (!runtimeDefinition.spec().equals(runtimeSpec)) {
                throw new IllegalStateException("Runtime with name " + runtimeSpec.name() + " already exists with different spec.");
            }
            return runtimeDefinition;
        }

        final ForgeUserDevRuntimeDefinition runtimeDefinition = create(runtimeSpec);
        runtimes.put(runtimeSpec.name(), runtimeDefinition);
        return runtimeDefinition;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private ForgeUserDevRuntimeDefinition create(ForgeUserDevRuntimeSpec runtimeSpec) {
        final McpRuntimeExtension mcpRuntimeExtension = project.getExtensions().getByType(McpRuntimeExtension.class);

        final Dependency userDevDependency = project.getDependencies().create(String.format("net.minecraftforge:forge:%s:userdev", runtimeSpec.forgeVersion()));
        final Configuration userDevConfiguration = project.getConfigurations().detachedConfiguration(userDevDependency);
        final ResolvedConfiguration resolvedUserDevConfiguration = userDevConfiguration.getResolvedConfiguration();
        final File userDevJar = resolvedUserDevConfiguration.getFiles().iterator().next();

        final File forgeDirectory = runtimeSpec.project().getLayout().getBuildDirectory().dir(String.format("forge/%s", runtimeSpec.name())).get().getAsFile();
        final File unpackedForgeDirectory = new File(forgeDirectory, "unpacked");

        unpackedForgeDirectory.mkdirs();

        FileUtils.unzip(userDevJar, unpackedForgeDirectory);

        final File userDevConfigFile = new File(unpackedForgeDirectory, "config.json");
        final UserDevConfigurationSpecV2 userDevConfigurationSpec = UserDevConfigurationSpecV2.get(userDevConfigFile);

        final Configuration userDevAdditionalDependenciesConfiguration = project.getConfigurations().detachedConfiguration();
        for (String dependencyCoordinate : userDevConfigurationSpec.getAdditionalDependencies()) {
            userDevAdditionalDependenciesConfiguration.getDependencies().add(project.getDependencies().create(dependencyCoordinate));
        }

        if (userDevConfigurationSpec.getParentName().isPresent()) {
            throw new IllegalStateException("Userdev configuration spec has parent name. As of now this is not supported!");
        }

        if (!userDevConfigurationSpec.getMcpVersion().isPresent()) {
            throw new IllegalStateException("Userdev configuration spec has no MCP version. As of now this is not supported!");
        }

        final String mcpVersion = Artifact.from(userDevConfigurationSpec.getMcpVersion().get()).getVersion();

        final McpRuntimeDefinition mcpRuntimeDefinition = mcpRuntimeExtension.maybeCreate((Action<McpRuntimeSpecBuilder>) builder -> {
            builder.withMcpVersion(mcpVersion)
                    .withSide(ArtifactSide.JOINED)
                    .withName(runtimeSpec.name())
                    .withAdditionalDependencies(project.files(userDevAdditionalDependenciesConfiguration))
                    .configureFromProject(runtimeSpec.configureProject());

            final TaskTreeAdapter atAndSASAdapter = createAccessTransformerAdapter(userDevConfigurationSpec.getAccessTransformerPaths(), unpackedForgeDirectory)
                    .andThen(createSideAnnotationStripperAdapter(userDevConfigurationSpec.getSideAnnotationStripperPaths(), unpackedForgeDirectory));

            builder.withPreTaskAdapter("decompile", atAndSASAdapter);

            final TaskTreeAdapter patchAdapter = createPatchAdapter(userDevConfigurationSpec.getSourcePatchesDirectory(), unpackedForgeDirectory);
            final Optional<TaskTreeAdapter> optionalInjectionAdapter = createInjectionAdapter(userDevConfigurationSpec.getInjectedFilesDirectory(), unpackedForgeDirectory);

            final TaskTreeAdapter resultingAdapter = optionalInjectionAdapter.map(inject -> inject.andThen(patchAdapter)).orElse(patchAdapter);
            final TaskTreeAdapter withForgeSourcesAdapter = userDevConfigurationSpec.getSourcesArtifactIdentifier().map(sources -> resultingAdapter.andThen(createInjectForgeSourcesAdapter(sources))).orElse(resultingAdapter);

            builder.withPostTaskAdapter("patch", withForgeSourcesAdapter);
        });

        return new ForgeUserDevRuntimeDefinition(
                runtimeSpec,
                mcpRuntimeDefinition,
                unpackedForgeDirectory,
                userDevConfigurationSpec,
                userDevAdditionalDependenciesConfiguration
        );
    }

    @NotNull
    private ForgeUserDevRuntimeSpec createSpec(Action<ForgeUserDevRuntimeSpecBuilder> configurator) {
        final ForgeUserDevRuntimeSpecBuilder builder = ForgeUserDevRuntimeSpecBuilder.from(project);
        configurator.execute(builder);
        return builder.build();
    }

    private Optional<TaskTreeAdapter> createInjectionAdapter(final Optional<String> injectionDirectory, final File unpackedForgeUserDevDirectory) {
        return injectionDirectory.map(s -> (spec, previousTasksOutput, dependentTaskConfigurationHandler) -> spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "injectUserDev"), Inject.class, task -> {
            task.getInjectionSource().set(previousTasksOutput.flatMap(WithOutput::getOutput));
            task.getInjectionDirectory().fileValue(new File(unpackedForgeUserDevDirectory, s));
        }));

    }

    private TaskTreeAdapter createPatchAdapter(final String patchDirectory, final File unpackForgeUserDevDirectory) {
        return (spec, previousTasksOutput, dependentTaskConfigurationHandler) -> spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "patchUserDev"), Patch.class, task -> {
            task.getInput().set(previousTasksOutput.flatMap(WithOutput::getOutput));
            task.getPatchDirectory().fileProvider(spec.project().provider(() -> new File(unpackForgeUserDevDirectory, patchDirectory)));
        });
    }

    private TaskTreeAdapter createInjectForgeSourcesAdapter(final String forgeSourcesCoordinate) {
        return (spec, previousTasksOutput, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends DownloadArtifact> downloadForgeSources = spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "downloadForgesSources"), DownloadArtifact.class, task -> {
                task.getArtifactCoordinate().set(forgeSourcesCoordinate);
            });

            dependentTaskConfigurationHandler.accept(downloadForgeSources);

            final TaskProvider<? extends UnpackZip> unzipForgeSources = spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "unzipForgesSources"), UnpackZip.class, task -> {
                task.getInputZip().set(downloadForgeSources.flatMap(DownloadArtifact::getOutput));
                task.dependsOn(downloadForgeSources);
            });

            dependentTaskConfigurationHandler.accept(unzipForgeSources);

            return spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "injectForgesSources"), Inject.class, task -> {
                task.getInjectionSource().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                task.getInjectionDirectory().set(unzipForgeSources.flatMap(UnpackZip::getUnpackingTarget));
                task.getInclusionFilter().set("net/**");
                task.dependsOn(unzipForgeSources);
            });
        };
    }

    private TaskTreeAdapter createAccessTransformerAdapter(final List<String> accessTransformerPaths, final File unpackedForgeUserDevDirectory) {
        final List<File> accessTransformerFiles = accessTransformerPaths.stream().map(path -> new File(unpackedForgeUserDevDirectory, path)).collect(Collectors.toList());
        return (spec, previousTasksOutput, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends AccessTransformer> accessTransformerTask = McpRuntimeUtils.createAccessTransformer(spec, "Forges", accessTransformerFiles, Collections.emptyList());
            accessTransformerTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            accessTransformerTask.configure(task -> task.dependsOn(previousTasksOutput));
            return accessTransformerTask;
        };
    }

    private TaskTreeAdapter createSideAnnotationStripperAdapter(final List<String> sideAnnotationStripperPaths, final File unpackedForgeUserDevDirectory) {
        final List<File> sideAnnotationStripperFiles = sideAnnotationStripperPaths.stream().map(path -> new File(unpackedForgeUserDevDirectory, path)).collect(Collectors.toList());
        return (spec, previousTasksOutput, dependentTaskConfigurationHandler) -> {
            final TaskProvider<? extends SideAnnotationStripper> sideAnnotationStripper = McpRuntimeUtils.createSideAnnotationStripper(spec, "Forges", sideAnnotationStripperFiles, Collections.emptyList());
            sideAnnotationStripper.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            sideAnnotationStripper.configure(task -> task.dependsOn(previousTasksOutput));
            return sideAnnotationStripper;
        };
    }
}

