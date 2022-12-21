package net.minecraftforge.gradle.vanilla.runtime.extensions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.spec.TaskTreeAdapter;
import net.minecraftforge.gradle.common.runtime.tasks.ArtifactProvider;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.common.util.VersionJson;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.ArtifactSide;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import net.minecraftforge.gradle.vanilla.runtime.spec.builder.VanillaRuntimeSpecBuilder;
import net.minecraftforge.gradle.vanilla.runtime.steps.*;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused"}) // API Design
public abstract class VanillaRuntimeExtension extends CommonRuntimeExtension<VanillaRuntimeSpec, VanillaRuntimeSpecBuilder, VanillaRuntimeDefinition> {

    @javax.inject.Inject
    public VanillaRuntimeExtension(Project project) {
        super(project);

        getForgeFlowerVersion().convention(Utils.FORGEFLOWER_VERSION);
        getFartVersion().convention(Utils.FART_VERSION);
        getAccessTransformerApplierVersion().convention(Utils.ACCESSTRANSFORMER_VERSION);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @NotNull
    protected VanillaRuntimeDefinition doCreate(final VanillaRuntimeSpec spec) {
        if (this.runtimes.containsKey(spec.name()))
            throw new IllegalArgumentException("Cannot register runtime with name '" + spec.name() + "' because it already exists");

        final Minecraft minecraftExtension = spec.project().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.project().getExtensions().getByType(MinecraftArtifactCache.class);

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final Map<GameArtifact, File> gameArtifacts = artifactCacheExtension.cacheGameVersion(spec.minecraftVersion(), spec.side());

        final VersionJson versionJson;
        try {
            versionJson = VersionJson.get(gameArtifacts.get(GameArtifact.VERSION_MANIFEST));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("Failed to read VersionJson from the launcher metadata for the minecraft version: %s", spec.minecraftVersion()), e);
        }

        final Configuration minecraftDependenciesConfiguration = spec.project().getConfigurations().detachedConfiguration();
        minecraftDependenciesConfiguration.setCanBeResolved(true);
        minecraftDependenciesConfiguration.setCanBeConsumed(false);
        for (VersionJson.Library library : versionJson.getLibraries()) {
            minecraftDependenciesConfiguration.getDependencies().add(
                    spec.project().getDependencies().create(library.getName())
            );
        }

        final File vanillaDirectory = spec.project().getLayout().getBuildDirectory().dir(String.format("vanilla/%s", spec.name())).get().getAsFile();
        final File runtimeWorkingDirectory = new File(vanillaDirectory, "runtime");
        final File stepsMcpDirectory = new File(vanillaDirectory, "steps");

        stepsMcpDirectory.mkdirs();

        final Map<String, File> data = Collections.emptyMap();
        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = buildDefaultArtifactProviderTasks(spec, vanillaDirectory);

        final TaskProvider<? extends ArtifactProvider> sourceJarTask = spec.project().getTasks().register("supplySourcesFor" + spec.name(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(runtimeWorkingDirectory, "sources.jar"));
            configureCommonMcpRuntimeTaskParameters(task, data, "supplySourcesFor" + spec.name(), spec, runtimeWorkingDirectory);
        });
        final TaskProvider<? extends ArtifactProvider> rawJarTask = spec.project().getTasks().register("supplyRawJarFor" + spec.name(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(runtimeWorkingDirectory, "raw.jar"));
            configureCommonMcpRuntimeTaskParameters(task, data, "supplyRawJarFor" + spec.name(), spec, runtimeWorkingDirectory);
        });

        return new VanillaRuntimeDefinition(spec, new LinkedHashMap<>(), sourceJarTask, rawJarTask, gameArtifacts, gameArtifactTasks, minecraftDependenciesConfiguration);
    }

    protected VanillaRuntimeSpecBuilder createBuilder() {
        return VanillaRuntimeSpecBuilder.create(getProject());
    }

    @Override
    protected void bakeDefinition(VanillaRuntimeDefinition definition) {
        final VanillaRuntimeSpec spec = definition.spec();

        final Minecraft minecraftExtension = spec.project().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.project().getExtensions().getByType(MinecraftArtifactCache.class);

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final File vanillaDirectory = spec.project().getLayout().getBuildDirectory().dir(String.format("vanilla/%s", spec.name())).get().getAsFile();
        final File runtimeWorkingDirectory = new File(vanillaDirectory, "runtime");
        final File stepsMcpDirectory = new File(vanillaDirectory, "steps");

        final StepData stepData = buildSteps();
        final List<IStep> steps = stepData.getSteps();

        TaskProvider<? extends WithOutput> currentInput = definition.gameArtifactProvidingTasks().get(spec.side().getGameArtifact());
        for (IStep step : steps) {
            if (spec.preTaskTypeAdapters().containsKey(step.getName())) {
                if (!spec.preTaskTypeAdapters().get(step.getName()).isEmpty()) {
                    int taskPreAdapterIndex = 0;
                    for (TaskTreeAdapter taskTreeAdapter : spec.preTaskTypeAdapters().get(step.getName())) {
                        final AtomicInteger additionalPreAdapterTasks = new AtomicInteger(0);
                        int currentTaskPreAdapterIndex = taskPreAdapterIndex;
                        final TaskProvider<? extends Runtime> modifiedTree = taskTreeAdapter.adapt(spec, currentInput, taskProvider -> taskProvider.configure(task -> configureCommonMcpRuntimeTaskParameters(task, Collections.emptyMap(), step.getName() + "PreAdapter" + currentTaskPreAdapterIndex + "-" + additionalPreAdapterTasks.getAndIncrement(), spec, runtimeWorkingDirectory)));
                        modifiedTree.configure(task -> configureCommonMcpRuntimeTaskParameters(task, Collections.emptyMap(), step.getName() + "PreAdapter" + currentTaskPreAdapterIndex + "-" + additionalPreAdapterTasks.getAndIncrement(), spec, runtimeWorkingDirectory));
                        currentInput = modifiedTree;
                        taskPreAdapterIndex++;
                    }
                }
            }

            AtomicInteger additionalTaskIndex = new AtomicInteger(0);
            TaskProvider<? extends Runtime> task = step.buildTask(definition, currentInput, minecraftCache, definition.taskOutputs(), definition.gameArtifacts(), definition.gameArtifactProvidingTasks(), taskProvider -> taskProvider.configure(additionalTask -> configureCommonMcpRuntimeTaskParameters(additionalTask, Collections.emptyMap(), step.getName() + "Additional" + additionalTaskIndex.getAndIncrement(), spec, runtimeWorkingDirectory)));

            task.configure((Runtime mcpRuntimeTask) -> configureCommonMcpRuntimeTaskParameters(mcpRuntimeTask, Collections.emptyMap(), step.getName(), spec, runtimeWorkingDirectory));

            if (!spec.postTypeAdapters().containsKey(step.getName())) {
                definition.taskOutputs().put(task.getName(), task);
            } else {
                int taskPostAdapterIndex = 0;
                for (TaskTreeAdapter taskTreeAdapter : spec.postTypeAdapters().get(step.getName())) {
                    final AtomicInteger additionalPostAdapterTasks = new AtomicInteger(0);
                    final int currentPostAdapterIndex = taskPostAdapterIndex;
                    final TaskProvider<? extends Runtime> taskProvider = taskTreeAdapter.adapt(spec, task, dependentTaskProvider -> dependentTaskProvider.configure(additionalTask -> configureCommonMcpRuntimeTaskParameters(additionalTask, Collections.emptyMap(), step.getName() + "PostAdapter" + currentPostAdapterIndex + "-" + additionalPostAdapterTasks.getAndIncrement(), spec, runtimeWorkingDirectory)));
                    taskProvider.configure(adaptedTask -> configureCommonMcpRuntimeTaskParameters(adaptedTask, Collections.emptyMap(), step.getName() + "PostAdapter" + currentPostAdapterIndex + "-" + additionalPostAdapterTasks.getAndIncrement(), spec, runtimeWorkingDirectory));
                    task = taskProvider;
                }

                definition.taskOutputs().put(task.getName(), task);
            }
        }

        final TaskProvider<? extends WithOutput> sourcesTask = Iterators.getLast( definition.taskOutputs().values().iterator());
        final TaskProvider<? extends WithOutput> rawTask =  definition.taskOutputs().get(stepData.getRawJarStep().getName());

        definition.sourceJarTask().configure(task -> {
            task.getInput().set(sourcesTask.flatMap(WithOutput::getOutput));
            task.dependsOn(sourcesTask);
        });
        definition.rawJarTask().configure(task -> {
            task.getInput().set(rawTask.flatMap(WithOutput::getOutput));
            task.dependsOn(rawTask);
        });
    }

    private StepData buildSteps() {
        final IStep rawJarStep = new ApplyAccessTransformerStep();
        final IStep sourcesStep = new DecompileStep();

        final List<IStep> steps = ImmutableList.<IStep>builder()
                .add(new CollectLibraryInformationStep())
                .add(new RenameStep())
                .add(rawJarStep)
                .add(sourcesStep)
                .build();

        return new StepData(steps, rawJarStep, sourcesStep);
    }

    public abstract Property<ArtifactSide> getSide();

    public abstract Property<String> getVersion();

    public abstract Property<String> getFartVersion();

    public abstract Property<String> getForgeFlowerVersion();

    public abstract Property<String> getAccessTransformerApplierVersion();

    private static final class StepData {
        private final List<IStep> steps;
        private final IStep rawJarStep;
        private final IStep sourceJarStep;

        private StepData(List<IStep> steps, IStep rawJarStep, IStep sourceJarStep) {
            this.steps = steps;
            this.rawJarStep = rawJarStep;
            this.sourceJarStep = sourceJarStep;
        }

        public List<IStep> getSteps() {
            return steps;
        }

        public IStep getRawJarStep() {
            return rawJarStep;
        }

        public IStep getSourceJarStep() {
            return sourceJarStep;
        }
    }
}
