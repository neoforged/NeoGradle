package net.neoforged.gradle.neoform.runtime;

import net.neoforged.gradle.common.runtime.tasks.ListLibraries;
import net.neoforged.gradle.dsl.common.runtime.CommonRuntimeBuilder;
import net.neoforged.gradle.dsl.common.runtime.definition.Outputs;
import net.neoforged.gradle.dsl.common.runtime.definition.TaskHandler;
import net.neoforged.gradle.dsl.common.runtime.spec.Specification;
import net.neoforged.gradle.dsl.common.runtime.spec.TaskTreeBuilder;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.dsl.neoform.configuration.LegacyNeoFormSdk;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormSdk;
import net.neoforged.gradle.neoform.runtime.tasks.InjectFromFileTreeSource;
import net.neoforged.gradle.neoform.runtime.tasks.InjectZipContent;
import net.neoforged.gradle.neoform.runtime.tasks.Patch;
import net.neoforged.gradle.neoform.runtime.tasks.StripJar;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class NeoFormRuntimeBuilder extends CommonRuntimeBuilder {

    private static final String DECOMPILE_FUNCTION = "decompile";

    private static final Map<String, StepOverride> FUNCTION_OVERRIDES = Map.of(
            DECOMPILE_FUNCTION, NeoFormRuntimeBuilder::createDecompile
    );
    private static final Set<LegacyNeoFormSdk.StepType> automaticallyIncludedTasks = EnumSet.of(
            LegacyNeoFormSdk.StepType.DOWNLOAD_CLIENT,
            LegacyNeoFormSdk.StepType.DOWNLOAD_SERVER,
            LegacyNeoFormSdk.StepType.DOWNLOAD_JSON,
            LegacyNeoFormSdk.StepType.DOWNLOAD_CLIENT_MAPPINGS,
            LegacyNeoFormSdk.StepType.DOWNLOAD_SERVER_MAPPINGS
    );

    private final NeoFormSdk sdk;

    public NeoFormRuntimeBuilder(Specification specification) {
        super(specification);
        this.sdk = NeoFormPublishingUtils.downloadAndParseSdkFile(specification.project(), specification.version());
    }

    @Override
    public TaskTreeBuilder.BuildResult build() {
        return build(specification.project(), specification.version());
    }

    protected TaskTreeBuilder.BuildResult build(final Project project, final String version) {
        final NeoFormSdk sdk = NeoFormPublishingUtils.downloadAndParseSdkFile(project, version);

        final TaskHandler taskHandler = new TaskHandler(project, task -> {
            throw new IllegalStateException("Not implemented");
        });

        sdk.getSteps().forEach((stepName, step) -> {
            taskHandler.register(stepName, NeoFormTask.class, task -> {
                task.setSdk(sdk);
                task.setStep(step);
            }
        });

        final Configuration dependencies = ConfigurationUtils.temporaryConfiguration(
                project,
                "neoformDependencies%s".formatted(version),
                NeoFormPublishingUtils.dependencies(project, version)
        );

        return new TaskTreeBuilder.BuildResult(dependencies, dependencies, taskHandler, Outputs.unknown());
    }

    protected interface StepOverride {
        public TaskProvider<? extends Runtime> create(final Project project,
                                                                final LegacyNeoFormSdk.Step step,
                                                                final TaskHandler handler);
    }

    @Nullable
    protected NamedDomainObjectProvider<? extends Runtime> processStep(final Project project,
                                                                       final LegacyNeoFormSdk.Step step,
                                                                       final TaskHandler handler) {

        if (step.getType() == LegacyNeoFormSdk.StepType.FUNCTION && step.getFunction() == null)
            throw new IllegalStateException("Function step must have a function name");

        if (step.getType() == LegacyNeoFormSdk.StepType.FUNCTION && FUNCTION_OVERRIDES.containsKey(step.getFunction()))
            return FUNCTION_OVERRIDES.get(step.getFunction()).create(project, step, handler);
        
        return switch (step.getType()) {
            case DOWNLOAD_CLIENT -> handler.task(GameArtifact.CLIENT_JAR);
            case DOWNLOAD_SERVER -> handler.task(GameArtifact.SERVER_JAR);
            case STRIP -> handler.register(
                    createTaskName(step.toString()),
                    StripJar.class,
                    task -> task.getInput().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, task)));;
            case LIST_LIBRARIES -> null;
            case INJECT -> null;
            case PATCH -> null;
            case DOWNLOAD_CLIENT_MAPPINGS -> null;
            case DOWNLOAD_SERVER_MAPPINGS -> null;
            case FUNCTION -> null;
        }
        
        
        
        
        
        switch (step.getType()) {
            case "decompile":
                return createDecompile(spec, step);
            case "downloadClient":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.CLIENT_JAR, a -> {
                    throw new IllegalStateException("Client Jar is required for this step, but was not provided");
                });
            case "downloadServer":
                return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.SERVER_JAR, a -> {
                    throw new IllegalStateException("Server Jar is required for this step, but was not provided");
                });
            case "strip":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), StripJar.class, task -> task.getInput().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, task)));
            case "listLibraries":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), ListLibraries.class, task -> {
                    task.getDownloadedVersionJsonFile()
                            .fileProvider(task.newProvider(cache.cacheVersionManifest(spec.getMinecraftVersion())));
                });
            case "inject":
                return spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, step.getName()), InjectZipContent.class, task -> {
                    task.getInjectionSource().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, task));
                    task.getInjectedSources()
                            .add(task.getRuntimeData().map(data -> data.get("inject"))
                                    .map(inject -> {
                                        final InjectFromFileTreeSource fileTreeSource = task.getObjectFactory()
                                                .newInstance(InjectFromFileTreeSource.class);
                                        fileTreeSource.getFiles().from(inject
                                                .matching(fileTree -> {
                                                    if (spec.getDistribution().equals(DistributionType.SERVER)) {
                                                        fileTree.include("**/server/**");
                                                    } else if (spec.getDistribution().equals(DistributionType.CLIENT)) {
                                                        fileTree.include("**/client/**");
                                                    }
                                                })
                                        );
                                        fileTreeSource.getTreePrefix().set(task.getSymbolicDataSources().map(data -> data.get("inject")));
                                        return fileTreeSource;
                                    })
                            );
                });
            case "patch":
                return spec.getProject().getTasks().register(
                        CommonRuntimeUtils.buildTaskName(spec, step.getName()),
                        Patch.class,
                        task -> {
                            task.getInput().fileProvider(NeoFormRuntimeUtils.getTaskInputFor(spec, tasks, step, task));
                            task.getPatchArchive().from(spec.getProject().fileTree(spec.getNeoFormArchive()));
                            task.getPatchDirectory().set(neoFormConfigV2.getData("patches", spec.getDistribution().getName()));
                        }
                );
        }
        if (neoFormConfigV2.getSpec() >= 2) {
            switch (step.getType()) {
                case "downloadClientMappings":
                    return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.CLIENT_MAPPINGS, a -> {
                        throw new IllegalStateException("Client Mappings are required for this step, but were not provided");
                    });
                case "downloadServerMappings":
                    return gameArtifactTaskProviders.computeIfAbsent(GameArtifact.SERVER_MAPPINGS, a -> {
                        throw new IllegalStateException("Server Mappings are required for this step, but were not provided");
                    });
            }
        }

        return null;
    }

    protected TaskProvider<? extends WithOutput> findInputFor(LegacyNeoFormSdk.Step step, TaskHandler handler) {

    }
}
