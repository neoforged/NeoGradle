package net.minecraftforge.gradle.common.deobfuscation;

import net.minecraftforge.gradle.common.extensions.DeobfuscationExtension;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.tasks.Execute;
import net.minecraftforge.gradle.common.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.base.util.DecompileUtils;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.Context;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.Constants;
import net.minecraftforge.gradle.dsl.base.util.GameArtifact;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.tasks.TaskProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public final class DependencyDeobfuscator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyDeobfuscator.class);
    private static final DependencyDeobfuscator INSTANCE = new DependencyDeobfuscator();

    public static DependencyDeobfuscator getInstance() {
        return INSTANCE;
    }

    private DependencyDeobfuscator() {
    }

    public void apply(final Project project) {
        final DependencyReplacement dependencyReplacer = project.getExtensions().getByType(DependencyReplacement.class);

        dependencyReplacer.getReplacementHandlers().create("obfuscatedDependencies", handler -> {
            handler.getReplacer().set(context -> {
                if (!(context.getDependency() instanceof ExternalModuleDependency)) {
                    return Optional.empty();
                }

                final Configuration resolver = context.getProject().getConfigurations().detachedConfiguration(context.getDependency());
                if (resolver.getResolvedConfiguration().getLenientConfiguration().getFiles().isEmpty()) {
                    return Optional.empty();
                }

                final Set<ResolvedDependency> dependencies = resolver.getResolvedConfiguration().getLenientConfiguration().getFirstLevelModuleDependencies();
                if (dependencies.size() == 0) {
                    return Optional.empty();
                }
                if (dependencies.size() != 1) {
                    LOGGER.warn("Dependency resolution for: " + context.getDependency() + " resulted in more then one resolved dependency. Skipping deobfuscation!");
                    return Optional.empty();
                }

                return determineReplacementOptions(context, dependencies.iterator().next());
            });
        });
    }

    private Optional<DependencyReplacementResult> determineReplacementOptions(final Context context, final ResolvedDependency resolvedDependency) {
        final Set<ResolvedArtifact> artifacts = resolvedDependency.getModuleArtifacts();
        if (artifacts.size() == 0) {
            return Optional.empty();
        }

        if (artifacts.size() != 1) {
            LOGGER.warn("Dependency resolution for: " + resolvedDependency.getName() + " resulted in more then one file. Can not deobfuscate!");
            return Optional.empty();
        }

        final ResolvedArtifact artifact = artifacts.iterator().next();
        final File file = artifact.getFile();

        try (final JarInputStream jarStream = new JarInputStream(Files.newInputStream(file.toPath()))) {
            Manifest mf = jarStream.getManifest();
            final boolean isObfuscated = mf != null && mf.getMainAttributes().containsKey("Obfuscated") && Boolean.parseBoolean(mf.getMainAttributes().getValue("Obfuscated"));
            final boolean obfuscatedByForgeGradle = mf != null && mf.getMainAttributes().containsKey("Obfuscated-By") && mf.getMainAttributes().getValue("Obfuscated-By").equals("ForgeGradle");
            if (isObfuscated && obfuscatedByForgeGradle) {
                final Set<ResolvedDependency> children = resolvedDependency.getChildren();
                final Map<ResolvedDependency, Optional<DependencyReplacementResult>> childResults = children.stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                child -> determineReplacementOptions(context, child)
                        ));

                final Collection<DependencyReplacementResult> dependentResults = childResults.values().stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList());

                final TaskProvider<ArtifactFromOutput> rawProvider = context.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName("deobfuscateRawFrom", resolvedDependency), ArtifactFromOutput.class, task -> {
                    task.getOutputFileName().set(file.getName());
                });

                final TaskProvider<ArtifactFromOutput> sourcesProvider = context.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName("deobfuscateSourcesFrom", resolvedDependency), ArtifactFromOutput.class, task -> {
                    task.getOutputFileName().set(file.getName().replace(".jar", "-sources.jar"));
                });

                final DependencyReplacementResult result = new DependencyReplacementResult(
                        context.getProject(),
                        name -> CommonRuntimeUtils.buildTaskName(name, resolvedDependency),
                        sourcesProvider,
                        rawProvider,
                        context.getProject().getConfigurations().detachedConfiguration(),
                        builder -> {
                            children.forEach(childDependency -> {
                                if (!childResults.containsKey(childDependency) || !childResults.get(childDependency).isPresent()) {
                                    builder.withDependency(depBuilder -> depBuilder.from(childDependency));
                                } else {
                                    final DependencyReplacementResult childResult = childResults.get(childDependency).get();
                                    builder.withDependency(depBuilder -> {
                                        childResult.getDependencyBuilderConfigurator().accept(depBuilder);
                                    });
                                }
                            });
                            builder.from(resolvedDependency);

                            final Mappings mappings = context.getProject().getExtensions().getByType(Mappings.class);
                            String deobfuscatedMappingsPrefix = mappings.getChannel().get().getDeobfuscationGroupSupplier().get();
                            if (deobfuscatedMappingsPrefix.trim().isEmpty()) {
                                deobfuscatedMappingsPrefix = mappings.getChannel().get().getName();
                            }
                            builder.setGroup("fg.deobf." + deobfuscatedMappingsPrefix + "." + resolvedDependency.getModuleGroup());
                        },
                        dependentResults,
                        builder -> {
                            builder.from(resolvedDependency);

                            final Mappings mappings = context.getProject().getExtensions().getByType(Mappings.class);
                            String deobfuscatedMappingsPrefix = mappings.getChannel().get().getDeobfuscationGroupSupplier().get();
                            if (deobfuscatedMappingsPrefix.trim().isEmpty()) {
                                deobfuscatedMappingsPrefix = mappings.getChannel().get().getName();
                            }

                            builder.setGroup("fg.deobf." + deobfuscatedMappingsPrefix + "." + resolvedDependency.getModuleGroup());
                        });

                final DeobfuscatingTaskConfiguration configuration = new DeobfuscatingTaskConfiguration(context, result, resolvedDependency, file);
                context.getProject().afterEvaluate(evaluatedProject -> bakeDependencyReplacement(evaluatedProject, configuration));

                return Optional.of(result);
            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read manifest for deobfuscation detection!", e);
            return Optional.empty();
        }
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

        final String postFix = deobfuscatingTaskConfiguration.resolvedDependency().getName();

        final Set<? extends Definition<?>> runtimeDefinitions = commonRuntimeExtension.findIn(deobfuscatingTaskConfiguration.context().getConfiguration());
        Definition<?> runtimeDefinition;
        if (runtimeDefinitions.size() != 1) {
            LOGGER.warn("Found {} runtime definitions for configuration {}!", runtimeDefinitions.size(), deobfuscatingTaskConfiguration.context().getConfiguration());
            LOGGER.warn("Raw jar deobfuscation might not deobfuscate to the correct version!");
        }
        runtimeDefinition = runtimeDefinitions.iterator().next();

        final MinecraftArtifactCache artifactCache = project.getExtensions().getByType(MinecraftArtifactCache.class);
        final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = artifactCache.cacheGameVersionTasks(project, new File(runtimeWorkingDirectory, "cache"), runtimeDefinition.getSpecification().getMinecraftVersion(), runtimeDefinition.getSpecification().getDistribution());

        final TaskProvider<? extends WithOutput> sourceFileProvider = project.getTasks().register(CommonRuntimeUtils.buildTaskName("provide", postFix), ArtifactFromOutput.class, task -> {
            task.getInput().fileValue(getFileFrom(deobfuscatingTaskConfiguration.resolvedDependency()).orElseThrow(() -> new IllegalStateException("Failed to get file from resolved dependency!")));
            task.getOutput().fileValue(new File(runtimeWorkingDirectory,  deobfuscatingTaskConfiguration.resolvedDependency().getName() + "-sources.jar"));
        });

        final TaskProvider<? extends WithOutput> rawJarDeobfuscator = mappingsExtension.getChannel().get()
                .getApplyCompiledMappingsTaskBuilder().get().build(
                        new TaskBuildingContext(
                                project,
                                postFix,
                                taskName -> CommonRuntimeUtils.buildTaskName(String.format("deobfuscate%s", StringUtils.capitalize(postFix)), taskName),
                                sourceFileProvider,
                                gameArtifactTasks,
                                runtimeDefinition.getMappingVersionData(),
                                new HashSet<>(),
                                runtimeDefinition)
                );

        deobfuscatingTaskConfiguration.dependencyReplacementResult().getRawJarTaskProvider().configure(task -> {
            if (!(task instanceof ArtifactFromOutput)) {
                throw new IllegalStateException("Expected task to be an instance of ArtifactFromOutput!");
            }

            final ArtifactFromOutput artifactFromOutput = (ArtifactFromOutput) task;
            artifactFromOutput.getInput().set(rawJarDeobfuscator.flatMap(WithOutput::getOutput));
            artifactFromOutput.dependsOn(rawJarDeobfuscator);
        });
    }

    private void createSourcesProvidingTask(final Project project, final DeobfuscatingTaskConfiguration deobfuscatingTaskConfiguration) {
        final Configuration sourcesConfiguration = project.getConfigurations().detachedConfiguration(project.getDependencies().create(this.createSourcesDependencyIdentifier(deobfuscatingTaskConfiguration.resolvedDependency().getModuleArtifacts().iterator().next())));
        final Optional<File> sourcesFileCandidate = getFileFrom(sourcesConfiguration.getResolvedConfiguration());

        final CommonRuntimeExtension<?,?,?> commonRuntimeExtension = project.getExtensions().getByType(CommonRuntimeExtension.class);
        final Mappings mappingsExtension = project.getExtensions().getByType(Mappings.class);

        final File runtimeWorkingDirectory = project.getLayout().getBuildDirectory().dir("dependencies").map(dir -> dir.dir("sources")).get().getAsFile();

        final String postFix = deobfuscatingTaskConfiguration.resolvedDependency().getName() + "Sources";

        TaskProvider<? extends WithOutput> generateSourcesTask;
        if (sourcesFileCandidate.isPresent()) {
            final Set<? extends Definition<?>> runtimeDefinitions = commonRuntimeExtension.findIn(deobfuscatingTaskConfiguration.context().getConfiguration());
            Definition<?> runtimeDefinition;
            if (runtimeDefinitions.size() != 1) {
                LOGGER.warn("Found {} runtime definitions for configuration {}!", runtimeDefinitions.size(), deobfuscatingTaskConfiguration.context().getConfiguration());
                LOGGER.warn("Source deobfuscation might not deobfuscate to the correct version!");
            }
            runtimeDefinition = runtimeDefinitions.iterator().next();

            final MinecraftArtifactCache artifactCache = project.getExtensions().getByType(MinecraftArtifactCache.class);
            final Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks = artifactCache.cacheGameVersionTasks(project, new File(runtimeWorkingDirectory, "cache"), runtimeDefinition.getSpecification().getMinecraftVersion(), runtimeDefinition.getSpecification().getDistribution());

            final TaskProvider<? extends WithOutput> sourceFileProvider = project.getTasks().register(CommonRuntimeUtils.buildTaskName("provide", postFix), ArtifactFromOutput.class, task -> {
                task.getInput().fileValue(sourcesFileCandidate.get());
                task.getOutput().fileValue(new File(runtimeWorkingDirectory,  deobfuscatingTaskConfiguration.resolvedDependency().getName() + "-sources.jar"));
            });

            generateSourcesTask = mappingsExtension.getChannel().get()
                    .getApplySourceMappingsTaskBuilder().get().build(
                            new TaskBuildingContext(
                                    project,
                                    postFix,
                                    taskName -> CommonRuntimeUtils.buildTaskName(String.format("deobfuscate%s", StringUtils.capitalize(postFix)), taskName),
                                    sourceFileProvider,
                                    gameArtifactTasks,
                                    runtimeDefinition.getMappingVersionData(),
                                    new HashSet<>(),
                                    runtimeDefinition)
                    );
        } else {
            LOGGER.warn("Could not find sources for dependency {} decompiling!", deobfuscatingTaskConfiguration.resolvedDependency().getName());

            final DeobfuscationExtension deobfuscationExtension = project.getExtensions().getByType(DeobfuscationExtension.class);

            final TaskProvider<? extends WithOutput> rawFileProvider = project.getTasks().register(CommonRuntimeUtils.buildTaskName("provide", postFix), ArtifactFromOutput.class, task -> {
                task.getInput().fileValue(getFileFrom(deobfuscatingTaskConfiguration.resolvedDependency()).orElseThrow(() -> new IllegalStateException("Could not find file for dependency " + deobfuscatingTaskConfiguration.resolvedDependency().getName())));
                task.getOutput().fileValue(new File(runtimeWorkingDirectory,  deobfuscatingTaskConfiguration.resolvedDependency().getName() + "-sources.jar"));
            });

            generateSourcesTask = project.getTasks().register(CommonRuntimeUtils.buildTaskName("decompile", postFix), Execute.class, task -> {
                task.getExecutingArtifact().set(deobfuscationExtension.getForgeFlowerVersion().map(version -> String.format(Constants.FORGEFLOWER_ARTIFACT_INTERPOLATION, version)));
                task.getJvmArguments().addAll(DecompileUtils.DEFAULT_JVM_ARGS);
                task.getProgramArguments().addAll(DecompileUtils.DEFAULT_PROGRAMM_ARGS);
                task.getArguments().set(CommonRuntimeUtils.buildArguments(
                        value -> Optional.empty(),
                        (String value) -> project.provider(() -> value),
                        Collections.emptyMap(),
                        task,
                        Optional.of(rawFileProvider)
                ));
            });
        }

        deobfuscatingTaskConfiguration.dependencyReplacementResult().getSourcesJarTaskProvider().configure(task -> {
            if (!(task instanceof ArtifactFromOutput)) {
                throw new IllegalStateException("Expected task to be an instance of ArtifactFromOutput!");
            }

            final ArtifactFromOutput artifactFromOutput = (ArtifactFromOutput) task;
            artifactFromOutput.getInput().set(generateSourcesTask.flatMap(WithOutput::getOutput));
            artifactFromOutput.dependsOn(generateSourcesTask);
        });
    }
}
