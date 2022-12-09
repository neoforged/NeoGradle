package net.minecraftforge.gradle.common.deobfuscation;

import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementContext;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementExtension;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.Artifact;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        final DependencyReplacementExtension dependencyReplacer = project.getExtensions().getByType(DependencyReplacementExtension.class);
        dependencyReplacer.getReplacementHandlers().add(new DependencyReplacementHandler() {

            @Override
            public @NotNull Optional<DependencyReplacementResult> get(@NotNull DependencyReplacementContext context) {
                if (!(context.dependency() instanceof ExternalModuleDependency)) {
                    return Optional.empty();
                }

                final Configuration resolver = context.project().getConfigurations().detachedConfiguration(context.dependency());
                if (resolver.getResolvedConfiguration().getLenientConfiguration().getFiles().isEmpty()) {
                    return Optional.empty();
                }

                final Set<ResolvedDependency> dependencies = resolver.getResolvedConfiguration().getLenientConfiguration().getFirstLevelModuleDependencies();
                if (dependencies.size() == 0) {
                    return Optional.empty();
                }
                if (dependencies.size() != 1) {
                    LOGGER.warn("Dependency resolution for: " + context.dependency() + " resulted in more then one resolved dependency. Skipping deobfuscation!");
                    return Optional.empty();
                }

                return determineReplacementOptions(dependencies.iterator().next());
            }


        });

    }

    private Optional<DependencyReplacementResult> determineReplacementOptions(final Project project, final ResolvedDependency resolvedDependency) {
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
            final boolean isObfuscated = mf.getMainAttributes().containsKey("Obfuscated") && Boolean.parseBoolean(mf.getMainAttributes().getValue("Obfuscated"));
            final boolean obfuscatedByForgeGradle = mf.getMainAttributes().containsKey("Obfuscated-By") && mf.getMainAttributes().getValue("Obfuscated-By").equals("ForgeGradle");
            if (isObfuscated && obfuscatedByForgeGradle) {
                final Set<ResolvedDependency> children = resolvedDependency.getChildren();
                final Map<ResolvedDependency, Optional<DependencyReplacementResult>> childResults = children.stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                child -> determineReplacementOptions(project, child)
                        ));

                final DependencyReplacementResult result =


            } else {
                return Optional.empty();
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read manifest for deobfuscation detection!", e);
            return Optional.empty();
        }
    }

    private TaskProvider<? extends ITaskWithOutput> createSourcesProvidingTask(final Project project, final ResolvedArtifact artifact) {
        final Configuration sourcesConfiguration = project.getConfigurations().detachedConfiguration(project.getDependencies().create(this.createSourcesDependencyIdentifier(artifact)));
        final Optional<File> sourcesFileCandidate = getFileFrom(sourcesConfiguration.getResolvedConfiguration());

        if (sourcesFileCandidate.isPresent()) {
            final String taskName = "provide" + artifact.get
            return project.getTasks().register()
        }

    }


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
        builder.append(":")

        if (classifier != null && !classifier.trim().isEmpty()) {
            builder.append(classifier)
                    .append("-")
        }

        builder.append("sources");

        if (extension != null && !extension.trim().isEmpty() && !extension.trim().toLowerCase(Locale.ROOT).equals("jar")) {
            builder.append("@")
                    .append(extension)
        }
    }

    private Optional<File> getFileFrom(final ResolvedConfiguration resolvedConfiguration) {
        final LenientConfiguration lenientConfiguration = resolvedConfiguration.getLenientConfiguration();
        final Set<ResolvedDependency> resolvedDependencies = lenientConfiguration.getFirstLevelModuleDependencies();
        if (resolvedDependencies.size() != 0)
            return Optional.empty();

        final ResolvedDependency resolvedDependency = resolvedDependencies.iterator().next();
        return getFileFrom(resolvedDependency);
    }

    private Optional<File> getFileFrom(final ResolvedDependency resolvedDependency) {
        final Set<ResolvedArtifact> artifacts = resolvedDependency.getModuleArtifacts();
        if (artifacts.size() != 1)
            return Optional.empty();

        final ResolvedArtifact artifact = artifacts.iterator().next();
        final Set<File> artifactFiles = artifact.getFile();
        if (artifactFiles.size() != 1)
            return Optional.empty();

        return Optional.of(artifactFiles.iterator().next());
    }
}
