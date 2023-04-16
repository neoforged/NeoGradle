package net.minecraftforge.gradle.vanilla.runtime.extensions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.util.BundledServerUtils;
import net.minecraftforge.gradle.common.util.FileCacheUtils;
import net.minecraftforge.gradle.common.util.VersionJson;
import net.minecraftforge.gradle.dsl.common.util.CacheFileSelector;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.dsl.common.util.NamingConstants;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.minecraftforge.gradle.dsl.common.tasks.ArtifactProvider;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.dsl.common.util.Constants;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpecification;
import net.minecraftforge.gradle.vanilla.runtime.steps.ApplyAccessTransformerStep;
import net.minecraftforge.gradle.vanilla.runtime.steps.CleanManifestStep;
import net.minecraftforge.gradle.vanilla.runtime.steps.CollectLibraryInformationStep;
import net.minecraftforge.gradle.vanilla.runtime.steps.DecompileStep;
import net.minecraftforge.gradle.vanilla.runtime.steps.IStep;
import net.minecraftforge.gradle.vanilla.runtime.steps.RenameStep;
import net.minecraftforge.gradle.vanilla.util.ServerLaunchInformation;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unused"}) // API Design
public abstract class VanillaRuntimeExtension extends CommonRuntimeExtension<VanillaRuntimeSpecification, VanillaRuntimeSpecification.Builder, VanillaRuntimeDefinition> {

    @javax.inject.Inject
    public VanillaRuntimeExtension(Project project) {
        super(project);

        getForgeFlowerVersion().convention(Constants.FORGEFLOWER_VERSION);
        getFartVersion().convention(Constants.FART_VERSION);
        getAccessTransformerApplierVersion().convention(Constants.ACCESSTRANSFORMER_VERSION);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @NotNull
    protected VanillaRuntimeDefinition doCreate(final VanillaRuntimeSpecification spec) {
        if (this.runtimes.containsKey(spec.getName()))
            throw new IllegalArgumentException("Cannot register runtime with name '" + spec.getName() + "' because it already exists");

        final Project project = spec.getProject();
        final Minecraft minecraftExtension = spec.getProject().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final Map<GameArtifact, File> gameArtifacts = artifactCacheExtension.cacheGameVersion(spec.getMinecraftVersion(), spec.getDistribution());
        if (gameArtifacts.containsKey(GameArtifact.SERVER_JAR)) {
            final File serverJar = gameArtifacts.get(GameArtifact.SERVER_JAR);
            if (BundledServerUtils.isBundledServer(serverJar)) {
                final File vanillaServerJar = new File(minecraftCache, String.format("minecraft_server.%s.jar", spec.getMinecraftVersion()));
                BundledServerUtils.extractBundledVersion(serverJar, vanillaServerJar);
                gameArtifacts.put(GameArtifact.SERVER_JAR, vanillaServerJar);
            }
        }

        final VersionJson versionJson;
        try {
            versionJson = VersionJson.get(gameArtifacts.get(GameArtifact.VERSION_MANIFEST));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("Failed to read VersionJson from the launcher metadata for the minecraft version: %s", spec.getMinecraftVersion()), e);
        }

        final Configuration minecraftDependenciesConfiguration = spec.getProject().getConfigurations().detachedConfiguration();
        minecraftDependenciesConfiguration.setCanBeResolved(true);
        minecraftDependenciesConfiguration.setCanBeConsumed(false);
        if (spec.getDistribution().isClient() || !BundledServerUtils.isBundledServer(gameArtifacts.get(GameArtifact.SERVER_JAR))) {
            for (VersionJson.Library library : versionJson.getLibraries()) {
                minecraftDependenciesConfiguration.getDependencies().add(
                        spec.getProject().getDependencies().create(library.getName())
                );
            }
        } else {
            BundledServerUtils.getBundledDependencies(gameArtifacts.get(GameArtifact.SERVER_JAR)).forEach(
                    dependency -> minecraftDependenciesConfiguration.getDependencies().add(
                            spec.getProject().getDependencies().create(dependency)));
        }

        final File vanillaDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("vanilla/%s", spec.getName())).get().getAsFile();
        final File runtimeWorkingDirectory = new File(vanillaDirectory, "runtime");
        final File stepsMcpDirectory = new File(vanillaDirectory, "steps");

        stepsMcpDirectory.mkdirs();

        final Map<String, File> data = Collections.emptyMap();
        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = buildDefaultArtifactProviderTasks(spec, vanillaDirectory);
        if (gameArtifactTasks.containsKey(GameArtifact.SERVER_JAR) && BundledServerUtils.isBundledServer(gameArtifacts.get(GameArtifact.SERVER_JAR))) {
            final TaskProvider<? extends WithOutput> serverJarTask = gameArtifactTasks.get(GameArtifact.SERVER_JAR);
            final TaskProvider<? extends WithOutput> extractedBundleTask = FileCacheUtils.createFileCacheEntryProvidingTask(
                    project, NamingConstants.Task.CACHE_VERSION_EXTRACTED_BUNDLE, spec.getMinecraftVersion(), vanillaDirectory, artifactCacheExtension.getCacheDirectory(), CacheFileSelector.forVersionJar(spec.getMinecraftVersion(), DistributionType.SERVER.getName()), () -> {
                        final File cacheFile = new File(artifactCacheExtension.getCacheDirectory().get().getAsFile(), CacheFileSelector.forVersionJar(spec.getMinecraftVersion(), DistributionType.SERVER.getName()).getCacheFileName());
                        BundledServerUtils.extractBundledVersion(serverJarTask.get().getOutput().get().getAsFile(), cacheFile);

                    }
            );

            extractedBundleTask.configure(task -> task.dependsOn(serverJarTask));
            gameArtifactTasks.put(GameArtifact.SERVER_JAR, extractedBundleTask);
        }

        final TaskProvider<? extends ArtifactProvider> sourceJarTask = spec.getProject().getTasks().register("supplySourcesFor" + spec.getName(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(runtimeWorkingDirectory, "sources.jar"));
        });
        final TaskProvider<? extends ArtifactProvider> rawJarTask = spec.getProject().getTasks().register("supplyRawJarFor" + spec.getName(), ArtifactProvider.class, task -> {
            task.getOutput().set(new File(runtimeWorkingDirectory, "raw.jar"));
        });

        final Optional<ServerLaunchInformation> launchInformation = spec.getDistribution().isClient() ? Optional.empty() : Optional.of(ServerLaunchInformation.from(gameArtifacts.get(GameArtifact.SERVER_JAR)));

        return new VanillaRuntimeDefinition(spec, new LinkedHashMap<>(), sourceJarTask, rawJarTask, gameArtifactTasks, minecraftDependenciesConfiguration, taskProvider -> taskProvider.configure(vanillaRuntimeTask -> {
            configureCommonRuntimeTaskParameters(vanillaRuntimeTask, data, CommonRuntimeUtils.buildStepName(spec, vanillaRuntimeTask.getName()), spec, vanillaDirectory);
        }), versionJson, createDownloadAssetsTasks(spec, data, runtimeWorkingDirectory, versionJson), createExtractNativesTasks(spec, data, runtimeWorkingDirectory, versionJson), launchInformation);
    }

    protected VanillaRuntimeSpecification.Builder createBuilder() {
        return VanillaRuntimeSpecification.Builder.from(getProject());
    }

    @Override
    protected void bakeDefinition(VanillaRuntimeDefinition definition) {
        final VanillaRuntimeSpecification spec = definition.getSpecification();

        final Minecraft minecraftExtension = spec.getProject().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final MinecraftArtifactCache artifactCacheExtension = spec.getProject().getExtensions().getByType(MinecraftArtifactCache.class);

        final File minecraftCache = artifactCacheExtension.getCacheDirectory().get().getAsFile();

        final File vanillaDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("vanilla/%s", spec.getName())).get().getAsFile();
        final File runtimeWorkingDirectory = new File(vanillaDirectory, "runtime");
        final File stepsMcpDirectory = new File(vanillaDirectory, "steps");

        final StepData stepData = buildSteps();
        final List<IStep> steps = stepData.getSteps();

        TaskProvider<? extends WithOutput> currentInput = definition.getGameArtifactProvidingTasks().get(spec.getDistribution().getGameArtifact());
        for (IStep step : steps) {
            if (spec.getPreTaskTypeAdapters().containsKey(step.getName())) {
                if (!spec.getPreTaskTypeAdapters().get(step.getName()).isEmpty()) {
                    int taskPreAdapterIndex = 0;
                    for (TaskTreeAdapter taskTreeAdapter : spec.getPreTaskTypeAdapters().get(step.getName())) {
                        final AtomicInteger additionalPreAdapterTasks = new AtomicInteger(0);
                        int currentTaskPreAdapterIndex = taskPreAdapterIndex;
                        final TaskProvider<? extends Runtime> modifiedTree = taskTreeAdapter.adapt(definition, currentInput, vanillaDirectory, definition.getGameArtifactProvidingTasks(), definition.getMappingVersionData(), taskProvider -> taskProvider.configure(task -> configureCommonRuntimeTaskParameters(task, Collections.emptyMap(), step.getName() + "PreAdapter" + currentTaskPreAdapterIndex + "-" + additionalPreAdapterTasks.getAndIncrement(), spec, runtimeWorkingDirectory)));
                        if (modifiedTree != null) {
                            modifiedTree.configure(task -> configureCommonRuntimeTaskParameters(task, Collections.emptyMap(), step.getName() + "PreAdapter" + currentTaskPreAdapterIndex + "-" + additionalPreAdapterTasks.getAndIncrement(), spec, runtimeWorkingDirectory));
                            currentInput = modifiedTree;
                            taskPreAdapterIndex++;
                        }
                    }
                }
            }

            AtomicInteger additionalTaskIndex = new AtomicInteger(0);
            TaskProvider<? extends Runtime> task = step.buildTask(definition, currentInput, minecraftCache, vanillaDirectory, definition.getTasks(), definition.getGameArtifactProvidingTasks(), taskProvider -> taskProvider.configure(additionalTask -> configureCommonRuntimeTaskParameters(additionalTask, Collections.emptyMap(), step.getName() + "Additional" + additionalTaskIndex.getAndIncrement(), spec, runtimeWorkingDirectory)));

            task.configure((Runtime mcpRuntimeTask) -> configureCommonRuntimeTaskParameters(mcpRuntimeTask, Collections.emptyMap(), step.getName(), spec, runtimeWorkingDirectory));

            if (!spec.getPostTypeAdapters().containsKey(step.getName())) {
                definition.getTasks().put(task.getName(), task);
            } else {
                int taskPostAdapterIndex = 0;
                for (TaskTreeAdapter taskTreeAdapter : spec.getPostTypeAdapters().get(step.getName())) {
                    final AtomicInteger additionalPostAdapterTasks = new AtomicInteger(0);
                    final int currentPostAdapterIndex = taskPostAdapterIndex++;
                    final TaskProvider<? extends Runtime> taskProvider = taskTreeAdapter.adapt(definition, task, vanillaDirectory, definition.getGameArtifactProvidingTasks(), definition.getMappingVersionData(), dependentTaskProvider -> dependentTaskProvider.configure(additionalTask -> configureCommonRuntimeTaskParameters(additionalTask, Collections.emptyMap(), step.getName() + "PostAdapter" + currentPostAdapterIndex + "-" + additionalPostAdapterTasks.getAndIncrement(), spec, runtimeWorkingDirectory)));
                    if (taskProvider != null) {
                        taskProvider.configure(adaptedTask -> configureCommonRuntimeTaskParameters(adaptedTask, Collections.emptyMap(), step.getName() + "PostAdapter" + currentPostAdapterIndex + "-" + additionalPostAdapterTasks.getAndIncrement(), spec, runtimeWorkingDirectory));
                        task = taskProvider;
                    }
                }

                definition.getTasks().put(task.getName(), task);
            }

            currentInput = task;
        }

        final TaskProvider<? extends WithOutput> sourcesTask = Iterators.getLast(definition.getTasks().values().iterator());
        final TaskProvider<? extends WithOutput> rawTask = definition.getTasks().get(stepData.getRawJarStep().getTaskName(definition));

        definition.getSourceJarTask().configure(task -> {
            task.getInput().set(sourcesTask.flatMap(WithOutput::getOutput));
            task.dependsOn(sourcesTask);
        });
        definition.getRawJarTask().configure(task -> {
            task.getInput().set(rawTask.flatMap(WithOutput::getOutput));
            task.dependsOn(rawTask);
        });
    }

    private StepData buildSteps() {
        final IStep rawJarStep = new CleanManifestStep();

        final IStep sourcesStep = new DecompileStep();

        final List<IStep> steps = ImmutableList.<IStep>builder()
                .add(new CollectLibraryInformationStep())
                .add(new RenameStep())
                .add(new ApplyAccessTransformerStep())
                .add(rawJarStep)
                .add(sourcesStep)
                .build();

        return new StepData(steps, rawJarStep, sourcesStep);
    }

    public abstract Property<DistributionType> getDefaultDistributionType();

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
