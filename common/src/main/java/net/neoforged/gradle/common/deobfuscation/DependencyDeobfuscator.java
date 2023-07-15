package net.neoforged.gradle.common.deobfuscation;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.neoforged.gradle.common.extensions.DeobfuscationExtension;
import net.neoforged.gradle.common.extensions.ForcedDependencyDeobfuscationExtension;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.CollectDependencyLibraries;
import net.neoforged.gradle.common.runtime.tasks.Execute;
import net.neoforged.gradle.common.tasks.ArtifactFromOutput;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.Context;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacer;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.Constants;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.dsl.common.util.ModuleReference;
import net.neoforged.gradle.util.DecompileUtils;
import net.neoforged.gradle.util.ResolvedDependencyUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.LenientConfiguration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * The logical handler for deobfuscating dependencies.
 * Handles the creation of the deobfuscation tasks and the replacement of the dependencies.
 * <p>
 * Relies on the {@link DependencyReplacement} extension to determine which dependencies to deobfuscate.
 *
 * @see DependencyReplacement
 * @see DependencyReplacer
 * @see DependencyReplacement
 * @see DependencyReplacementResult
 * @see Context
 */
public abstract class DependencyDeobfuscator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyDeobfuscator.class);

    private final Project project;

    private final Map<ModuleReference, Optional<DependencyReplacementResult>> setupReferences = Maps.newHashMap();

    @Inject
    public DependencyDeobfuscator(Project project) {
        this.project = project;

        this.apply();
    }

    /**
     * Applies the deobfuscation handler to the given project.
     */
    private void apply() {
        //Get the replacement handler.
        final DependencyReplacement dependencyReplacer = project.getExtensions().getByType(DependencyReplacement.class);

        //Register our replacement handler.
        dependencyReplacer.getReplacementHandlers().create("obfuscatedDependencies", handler -> {
            handler.getReplacer().set(context -> {
                //We only want to replace external dependencies.
                if (!(context.getDependency() instanceof ExternalModuleDependency)) {
                    return Optional.empty();
                }

                //We only want to replace dependencies that actually exist.
                final Configuration resolver = ConfigurationUtils.temporaryConfiguration(project, context.getDependency());
                if (resolver.getResolvedConfiguration().getLenientConfiguration().getFiles().isEmpty()) {
                    //No files, so we can't replace it. -> Might be a resolution failure!
                    return Optional.empty();
                }

                //We only want to replace dependencies that have a single resolved dependency.
                final Set<ResolvedDependency> dependencies = resolver.getResolvedConfiguration().getLenientConfiguration().getFirstLevelModuleDependencies();
                if (dependencies.size() == 0) {
                    //No dependencies, so we can't replace it. -> Might be a resolution failure!
                    return Optional.empty();
                }
                if (dependencies.size() != 1) {
                    //More than one dependency, so we can't replace it.
                    LOGGER.warn("Dependency resolution for: " + context.getDependency() + " resulted in more then one resolved dependency. Skipping deobfuscation!");
                    return Optional.empty();
                }

                final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = project.getExtensions().getByType(ForcedDependencyDeobfuscationExtension.class);
                final ResolvedDependency resolvedDependency = dependencies.iterator().next();
                //Handle replacement of the resolved dependency.
                return determineCachedReplacementOptions(context.getConfiguration(), forcedDependencyDeobfuscationExtension.shouldDeobfuscate(context.getDependency()), resolvedDependency);
            });
        });
    }

    private Optional<DependencyReplacementResult> determineCachedReplacementOptions(final Configuration configuration, final boolean forceDeobfuscation, final ResolvedDependency resolvedDependency) {
        //Get all the artifacts that need to be processed.
        final Set<ResolvedArtifact> artifacts = resolvedDependency.getModuleArtifacts();
        if (artifacts.size() == 0) {
            //No artifacts found, so we can't replace it. -> Might be a resolution failure!
            return Optional.empty();
        }

        if (artifacts.size() != 1) {
            //More than one artifact, so we can't replace it.
            LOGGER.warn("Dependency resolution for: " + resolvedDependency.getName() + " resulted in more then one file. Can not deobfuscate!");
            return Optional.empty();
        }

        final ResolvedArtifact artifact = artifacts.iterator().next();
        final ModuleReference reference = new ModuleReference(resolvedDependency.getModuleGroup(), resolvedDependency.getModuleName(), resolvedDependency.getModuleVersion(), artifact.getExtension(), artifact.getClassifier());

        //Check if we have already setup this reference.
        if (setupReferences.containsKey(reference)) {
            //We have already setup this reference, so we can skip it.
            return setupReferences.get(reference);
        }

        final Optional<DependencyReplacementResult> result = determineReplacementOptions(project, configuration, forceDeobfuscation, resolvedDependency, artifact);
        setupReferences.put(reference, result);
        return result;
    }

    private Optional<DependencyReplacementResult> determineReplacementOptions(final Project project, final Configuration configuration, boolean forceDeobfuscation, final ResolvedDependency resolvedDependency, ResolvedArtifact artifact) {
        //Grab the one artifact, and its file.
        final File file = artifact.getFile();


        //Check if the artifact is obfuscated.
        //The try-with-resources catches any IOExceptions that might occur, in turn validating that we are talking about an actual jar file.
        try (final JarInputStream jarStream = new JarInputStream(Files.newInputStream(file.toPath()))) {
            Manifest mf = jarStream.getManifest();
            //Check if we have a valid manifest.
            final boolean isObfuscated = mf != null && mf.getMainAttributes().containsKey(new Attributes.Name("Obfuscated")) && Boolean.parseBoolean(mf.getMainAttributes().getValue("Obfuscated"));
            final boolean obfuscatedByForgeGradle = mf != null && mf.getMainAttributes().containsKey(new Attributes.Name("Obfuscated-By")) && mf.getMainAttributes().getValue("Obfuscated-By").equals("ForgeGradle");

            if ((isObfuscated && obfuscatedByForgeGradle) || forceDeobfuscation) {
                return createDeobfuscationDependencyReplacementResult(project, configuration, forceDeobfuscation, resolvedDependency, file);
            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            //Failed to read the jar file, so we can't replace it.
            LOGGER.warn("Failed to read manifest for deobfuscation detection!", e);

            if (forceDeobfuscation) {
                return createDeobfuscationDependencyReplacementResult(project, configuration, true, resolvedDependency, file);
            }

            return Optional.empty();
        }
    }

    @NotNull
    private Optional<DependencyReplacementResult> createDeobfuscationDependencyReplacementResult(final Project project, final Configuration configuration, final boolean forceDeobfuscation, ResolvedDependency resolvedDependency, File file) {
        //We have an obfuscated artifact, so we need to deobfuscate it.
        final Set<ResolvedDependency> children = resolvedDependency.getChildren();
        final Map<ResolvedDependency, Optional<DependencyReplacementResult>> childResults = children.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        child -> determineCachedReplacementOptions(configuration, forceDeobfuscation, child)
                ));

        final Collection<DependencyReplacementResult> dependentResults = childResults.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        final TaskProvider<ArtifactFromOutput> rawProvider = project.getTasks().register(CommonRuntimeUtils.buildTaskName("deobfuscateRawFrom", resolvedDependency), ArtifactFromOutput.class, task -> {
            task.getOutputFileName().set(file.getName());
        });

        final TaskProvider<ArtifactFromOutput> sourcesProvider = project.getTasks().register(CommonRuntimeUtils.buildTaskName("deobfuscateSourcesFrom", resolvedDependency), ArtifactFromOutput.class, task -> {
            task.getOutputFileName().set(file.getName().replace(".jar", "-sources.jar"));
        });

        final DependencyReplacementResult result = new DependencyReplacementResult(
                project,
                name -> name,
                sourcesProvider,
                rawProvider,
                ConfigurationUtils.temporaryConfiguration(project),
                builder -> {
                    children.forEach(childDependency -> {
                        if (!childResults.containsKey(childDependency) || !childResults.get(childDependency).isPresent()) {
                            builder.withDependency(depBuilder -> depBuilder.from(childDependency));
                        } else {
                            final DependencyReplacementResult childResult = childResults.get(childDependency).get();
                            builder.withProcessedDependency(b -> {
                                childResult.getDependencyMetadataConfigurator().accept(b);
                            });
                        }
                    });
                    builder.from(resolvedDependency);

                    final Mappings mappings = project.getExtensions().getByType(Mappings.class);
                    String deobfuscatedMappingsPrefix = mappings.getChannel().get().getDeobfuscationGroupSupplier().get();
                    if (deobfuscatedMappingsPrefix.trim().isEmpty()) {
                        deobfuscatedMappingsPrefix = mappings.getChannel().get().getName();
                    }
                    builder.setGroup("fg.deobf." + deobfuscatedMappingsPrefix + "." + resolvedDependency.getModuleGroup());
                },
                dependentResults,
                Sets::newHashSet);

        final DeobfuscatingTaskConfiguration taskConfig = new DeobfuscatingTaskConfiguration(configuration, result, resolvedDependency, file, childResults);
        project.afterEvaluate(evaluatedProject -> bakeDependencyReplacement(evaluatedProject, taskConfig));

        return Optional.of(result);
    }


    @SuppressWarnings("ConstantValue")
    private String createSourcesDependencyIdentifier(final ResolvedArtifact artifact) {
        final StringBuilder builder = new StringBuilder();

        final String group = artifact.getModuleVersion().getId().getGroup();
        final String artifactName = artifact.getModuleVersion().getId().getName();
        final String version = artifact.getModuleVersion().getId().getVersion();
        final String extension = artifact.getExtension();
        final String classifier = artifact.getClassifier();

        if (group != null && !group.trim().isEmpty()) {
            builder.append(group);
        }

        builder.append(":");
        builder.append(artifactName);
        builder.append(":");
        builder.append(version);
        builder.append(":");

        if (classifier != null && !classifier.trim().isEmpty()) {
            builder.append(classifier)
                    .append("-");
        }

        builder.append("sources");

        if (extension != null && !extension.trim().isEmpty() && !extension.trim().toLowerCase(Locale.ROOT).equals("jar")) {
            builder.append("@")
                    .append(extension);
        }

        return builder.toString();
    }

    private Optional<File> getFileFrom(final ResolvedConfiguration resolvedConfiguration) {
        final LenientConfiguration lenientConfiguration = resolvedConfiguration.getLenientConfiguration();
        final Set<ResolvedDependency> resolvedDependencies = lenientConfiguration.getFirstLevelModuleDependencies();
        if (resolvedDependencies.size() != 1)
            return Optional.empty();

        final ResolvedDependency resolvedDependency = resolvedDependencies.iterator().next();
        return getFileFrom(resolvedDependency);
    }

    private Optional<File> getFileFrom(final ResolvedDependency resolvedDependency) {
        final Set<ResolvedArtifact> artifacts = resolvedDependency.getModuleArtifacts();
        if (artifacts.size() != 1)
            return Optional.empty();

        final ResolvedArtifact artifact = artifacts.iterator().next();
        final File artifactFile = artifact.getFile();
        return Optional.of(artifactFile);
    }

    private void bakeDependencyReplacement(final Project project, final DeobfuscatingTaskConfiguration configuration) {
        createRawProvidingTask(project, configuration);
        createSourcesProvidingTask(project, configuration);
    }

    private void createRawProvidingTask(final Project project, final DeobfuscatingTaskConfiguration deobfuscatingTaskConfiguration) {
        final CommonRuntimeExtension<?,?,?> commonRuntimeExtension = project.getExtensions().getByType(CommonRuntimeExtension.class);
        final Mappings mappingsExtension = project.getExtensions().getByType(Mappings.class);

        final File runtimeWorkingDirectory = project.getLayout().getBuildDirectory().dir("dependencies").map(dir -> dir.dir("raw")).get().getAsFile();

        final String postFix = deobfuscatingTaskConfiguration.getResolvedDependency().getName();

        final Set<? extends Definition<?>> runtimeDefinitions = commonRuntimeExtension.findIn(deobfuscatingTaskConfiguration.getConfiguration());
        Definition<?> runtimeDefinition;
        if (runtimeDefinitions.size() != 1) {
            LOGGER.warn("Found {} runtime definitions for configuration {}!", runtimeDefinitions.size(), deobfuscatingTaskConfiguration.getConfiguration());
            LOGGER.warn("Raw jar deobfuscation might not deobfuscate to the correct version!");
        }
        runtimeDefinition = runtimeDefinitions.iterator().next();

        final MinecraftArtifactCache artifactCache = project.getExtensions().getByType(MinecraftArtifactCache.class);
        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = artifactCache.cacheGameVersionTasks(project, new File(runtimeWorkingDirectory, "cache"), runtimeDefinition.getSpecification().getMinecraftVersion(), runtimeDefinition.getSpecification().getDistribution());

        final TaskProvider<? extends WithOutput> sourceFileProvider = project.getTasks().register(CommonRuntimeUtils.buildTaskName("provide", postFix), ArtifactFromOutput.class, task -> {
            task.getInput().fileValue(getFileFrom(deobfuscatingTaskConfiguration.getResolvedDependency()).orElseThrow(() -> new IllegalStateException("Failed to get file from resolved dependency!")));
            task.getOutput().fileValue(new File(runtimeWorkingDirectory, ResolvedDependencyUtils.toFileName(deobfuscatingTaskConfiguration.getResolvedDependency()) + "-sources.jar"));
        });

        final TaskProvider<? extends Runtime> generateDependencyLibraries = project.getTasks().register(CommonRuntimeUtils.buildTaskName("collectLibraries", deobfuscatingTaskConfiguration.getResolvedDependency()), CollectDependencyLibraries.class, task -> {
            task.getBaseLibraryFile().set(runtimeDefinition.getListLibrariesTaskProvider().flatMap(WithOutput::getOutput));

            final List<File> dependencies = deobfuscatingTaskConfiguration.getResolvedDependency().getAllModuleArtifacts().stream().map(ResolvedArtifact::getFile).collect(Collectors.toList());
            dependencies.removeAll(deobfuscatingTaskConfiguration.getResolvedDependency().getModuleArtifacts().stream().map(ResolvedArtifact::getFile).collect(Collectors.toList()));

            task.getDependencyFiles().from(dependencies);
            task.getDependencyFiles().from(runtimeDefinition.getRuntimeMappedRawJarTaskProvider().flatMap(WithOutput::getOutput));

            task.dependsOn(runtimeDefinition.getListLibrariesTaskProvider());
            task.dependsOn(runtimeDefinition.getRuntimeMappedRawJarTaskProvider());

            deobfuscatingTaskConfiguration.getChildResults().values()
                    .stream().filter(Optional::isPresent).map(Optional::get)
                    .map(DependencyReplacementResult::getRawJarTaskProvider)
                    .forEach(task::dependsOn);
        });
        runtimeDefinition.configureAssociatedTask(generateDependencyLibraries);

        final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = new HashSet<>();
        final TaskProvider<? extends Runtime> rawJarDeobfuscator = mappingsExtension.getChannel().get()
                .getApplyCompiledMappingsTaskBuilder().get().build(
                        new TaskBuildingContext(
                                project,
                                postFix,
                                taskName -> CommonRuntimeUtils.buildTaskName(String.format("deobfuscate%s", StringUtils.capitalize(postFix)), taskName),
                                sourceFileProvider,
                                gameArtifactTasks,
                                runtimeDefinition.getMappingVersionData(),
                                additionalRuntimeTasks,
                                runtimeDefinition,
                                generateDependencyLibraries)
                );

        runtimeDefinition.configureAssociatedTask(rawJarDeobfuscator);
        additionalRuntimeTasks.forEach(runtimeDefinition::configureAssociatedTask);

        deobfuscatingTaskConfiguration.getDependencyReplacementResult().getRawJarTaskProvider().configure(task -> {
            if (!(task instanceof ArtifactFromOutput)) {
                throw new IllegalStateException("Expected task to be an instance of ArtifactFromOutput!");
            }

            final ArtifactFromOutput artifactFromOutput = (ArtifactFromOutput) task;
            artifactFromOutput.getInput().set(rawJarDeobfuscator.flatMap(WithOutput::getOutput));
            artifactFromOutput.dependsOn(rawJarDeobfuscator);
        });
    }

    private void createSourcesProvidingTask(final Project project, final DeobfuscatingTaskConfiguration deobfuscatingTaskConfiguration) {
        final Configuration sourcesConfiguration = ConfigurationUtils.temporaryConfiguration(project, project.getDependencies().create(this.createSourcesDependencyIdentifier(deobfuscatingTaskConfiguration.getResolvedDependency().getModuleArtifacts().iterator().next())));
        final Optional<File> sourcesFileCandidate = getFileFrom(sourcesConfiguration.getResolvedConfiguration());

        final CommonRuntimeExtension<?,?,?> commonRuntimeExtension = project.getExtensions().getByType(CommonRuntimeExtension.class);
        final Mappings mappingsExtension = project.getExtensions().getByType(Mappings.class);

        final File runtimeWorkingDirectory = project.getLayout().getBuildDirectory().dir("dependencies").map(dir -> dir.dir("sources")).get().getAsFile();

        final String postFix = deobfuscatingTaskConfiguration.getResolvedDependency().getName() + "Sources";

        TaskProvider<? extends Runtime> generateSourcesTask;
        if (sourcesFileCandidate.isPresent()) {
            final Set<? extends Definition<?>> runtimeDefinitions = commonRuntimeExtension.findIn(deobfuscatingTaskConfiguration.getConfiguration());
            Definition<?> runtimeDefinition;
            if (runtimeDefinitions.size() != 1) {
                LOGGER.warn("Found {} runtime definitions for configuration {}!", runtimeDefinitions.size(), deobfuscatingTaskConfiguration.getConfiguration());
                LOGGER.warn("Source deobfuscation might not deobfuscate to the correct version!");
            }
            runtimeDefinition = runtimeDefinitions.iterator().next();

            final MinecraftArtifactCache artifactCache = project.getExtensions().getByType(MinecraftArtifactCache.class);
            final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = artifactCache.cacheGameVersionTasks(project, new File(runtimeWorkingDirectory, "cache"), runtimeDefinition.getSpecification().getMinecraftVersion(), runtimeDefinition.getSpecification().getDistribution());

            final TaskProvider<? extends WithOutput> sourceFileProvider = project.getTasks().register(CommonRuntimeUtils.buildTaskName("provide", postFix), ArtifactFromOutput.class, task -> {
                task.getInput().fileValue(sourcesFileCandidate.get());
                task.getOutput().fileValue(new File(runtimeWorkingDirectory, ResolvedDependencyUtils.toFileName(deobfuscatingTaskConfiguration.getResolvedDependency()) + "-sources.jar"));
            });

            final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = new HashSet<>();
            generateSourcesTask = mappingsExtension.getChannel().get()
                    .getApplySourceMappingsTaskBuilder().get().build(
                            new TaskBuildingContext(
                                    project,
                                    postFix,
                                    taskName -> CommonRuntimeUtils.buildTaskName(String.format("deobfuscate%s", StringUtils.capitalize(postFix)), taskName),
                                    sourceFileProvider,
                                    gameArtifactTasks,
                                    runtimeDefinition.getMappingVersionData(),
                                    additionalRuntimeTasks,
                                    runtimeDefinition)
                    );

            runtimeDefinition.configureAssociatedTask(generateSourcesTask);
            additionalRuntimeTasks.forEach(runtimeDefinition::configureAssociatedTask);
        } else {
            LOGGER.warn("Could not find sources for dependency {} decompiling!", deobfuscatingTaskConfiguration.getResolvedDependency().getName());

            final DeobfuscationExtension deobfuscationExtension = project.getExtensions().getByType(DeobfuscationExtension.class);

            final TaskProvider<? extends WithOutput> rawFileProvider = project.getTasks().register(CommonRuntimeUtils.buildTaskName("provide", postFix), ArtifactFromOutput.class, task -> {
                task.getInput().fileValue(getFileFrom(deobfuscatingTaskConfiguration.getResolvedDependency()).orElseThrow(() -> new IllegalStateException("Could not find file for dependency " + deobfuscatingTaskConfiguration.getResolvedDependency().getName())));
                task.getOutput().fileValue(new File(runtimeWorkingDirectory, ResolvedDependencyUtils.toFileName(deobfuscatingTaskConfiguration.getResolvedDependency()) + "-sources.jar"));
            });

            generateSourcesTask = project.getTasks().register(CommonRuntimeUtils.buildTaskName("decompile", postFix), Execute.class, task -> {
                task.getExecutingArtifact().set(deobfuscationExtension.getForgeFlowerVersion().map(version -> String.format(Constants.FORGEFLOWER_ARTIFACT_INTERPOLATION, version)));
                task.getJvmArguments().addAll(DecompileUtils.DEFAULT_JVM_ARGS);
                task.getProgramArguments().addAll(DecompileUtils.DEFAULT_PROGRAMM_ARGS);
                task.getArguments().putAll(CommonRuntimeUtils.buildArguments(
                        value -> Optional.empty(),
                        (String value) -> project.provider(() -> value),
                        Collections.emptyMap(),
                        task,
                        Optional.of(rawFileProvider)
                ));
            });
        }

        deobfuscatingTaskConfiguration.getDependencyReplacementResult().getSourcesJarTaskProvider().configure(task -> {
            if (!(task instanceof ArtifactFromOutput)) {
                throw new IllegalStateException("Expected task to be an instance of ArtifactFromOutput!");
            }

            final ArtifactFromOutput artifactFromOutput = (ArtifactFromOutput) task;
            artifactFromOutput.getInput().set(generateSourcesTask.flatMap(WithOutput::getOutput));
            artifactFromOutput.dependsOn(generateSourcesTask);
        });
    }
}
