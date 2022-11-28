package net.minecraftforge.gradle.common.extensions.dependency.replacement;

import com.google.common.collect.Sets;
import groovy.lang.GroovyObjectSupport;
import net.minecraftforge.gradle.common.extensions.IvyDummyRepositoryExtension;
import net.minecraftforge.gradle.common.ide.IdeManager;
import net.minecraftforge.gradle.common.repository.IvyDummyRepositoryEntry;
import net.minecraftforge.gradle.common.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.tasks.RawAndSourceCombiner;
import net.minecraftforge.gradle.common.util.IConfigurableObject;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public abstract class DependencyReplacementExtension extends GroovyObjectSupport implements IConfigurableObject<DependencyReplacementExtension> {

    private final Project project;

    @Inject
    public DependencyReplacementExtension(Project project) {
        this.project = project;
        this.project.getConfigurations().configureEach(configuration -> configuration.getDependencies().whenObjectAdded(dependency -> handleDependency(configuration, dependency)));
    }

    public abstract SetProperty<DependencyReplacementHandler> getReplacementHandlers();

    @SuppressWarnings("OptionalGetWithoutIsPresent") //Two lines above the get....
    private void handleDependency(final Configuration configuration, final Dependency dependency) {
        Set<DependencyReplacementHandler> replacementHandlers = Sets.newHashSet(getReplacementHandlers().get());
        replacementHandlers.stream()
                .map(handler -> handler.get(new DependencyReplacementContext(project, configuration, dependency)))
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get)
                .ifPresent(result -> {
                    handleDependencyReplacement(configuration, dependency, result);
                });
    }

    private void handleDependencyReplacement(Configuration configuration, Dependency dependency, DependencyReplacementResult result) {
        configuration.getDependencies().remove(dependency);

        if (IdeManager.getInstance().isIdeImportInProgress()) {
            handleDependencyReplacementForIde(configuration, dependency, result);
        }

        handleDependencyReplacementForGradle(configuration, dependency, result);
    }

    private void handleDependencyReplacementForGradle(Configuration configuration, Dependency dependency, DependencyReplacementResult result) {
        final String artifactSelectionTaskName = result.taskNameBuilder().apply("selectRawArtifact");
        final TaskProvider<? extends ArtifactFromOutput> rawArtifactSelectionTask = project.getTasks().register(artifactSelectionTaskName, ArtifactFromOutput.class, artifactFromOutput -> {
            artifactFromOutput.setGroup("mcp");
            artifactFromOutput.setDescription(String.format("Selects the raw artifact from the %s dependency", dependency.toString()));

            artifactFromOutput.getInput().set(result.rawJarTaskProvider().flatMap(ITaskWithOutput::getOutput));
            artifactFromOutput.dependsOn(result.rawJarTaskProvider());
        });

        result.additionalDependenciesConfiguration().getDependencies().forEach(dependentDependency -> {
            configuration.getDependencies().add(dependentDependency);
        });
        configuration.getDependencies().add(project.getDependencies().create(project.files(rawArtifactSelectionTask)));
    }

    private void handleDependencyReplacementForIde(Configuration configuration, Dependency dependency, DependencyReplacementResult result) {
        if (!(dependency instanceof ExternalModuleDependency)) {
            return;
        }

        final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependency;

        final IvyDummyRepositoryExtension extension = project.getExtensions().getByType(IvyDummyRepositoryExtension.class);
        final Provider<Directory> repoBaseDir = extension.createRepoBaseDir();
        final IvyDummyRepositoryEntry entry;
        try {
            entry = extension.withDependency(builder -> {
                builder.from(externalModuleDependency);
                result.dependencyMetadataConfigurator().accept(builder);

                result.additionalDependenciesConfiguration().getDependencies()
                        .stream()
                        .filter(ExternalModuleDependency.class::isInstance)
                        .map(ExternalModuleDependency.class::cast)
                        .forEach(additionalDependency -> builder.withDependency(depBuilder -> depBuilder.from(additionalDependency)));
            });
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException(String.format("Failed to create the dummy dependency for: %s", dependency.toString()), e);
        }

        final String dependencyExporterTaskName = result.taskNameBuilder().apply("combined");
        final TaskProvider<? extends RawAndSourceCombiner> rawAndSourceCombinerTask = project.getTasks().register(dependencyExporterTaskName, RawAndSourceCombiner.class, rawAndSourceCombiner -> {
            rawAndSourceCombiner.setGroup("mcp");
            rawAndSourceCombiner.setDescription(String.format("Combines the raw and sources jars into a single task execution tree for: %s", dependency.toString()));

            rawAndSourceCombiner.getRawJarInput().set(result.rawJarTaskProvider().flatMap(ITaskWithOutput::getOutput));
            rawAndSourceCombiner.getSourceJarInput().set(result.sourcesJarTaskProvider().flatMap(ITaskWithOutput::getOutput));

            rawAndSourceCombiner.getRawJarOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.artifactPath(dir.getAsFile().toPath()).toFile())));
            rawAndSourceCombiner.getSourceJarOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.asSources().artifactPath(dir.getAsFile().toPath()).toFile())));
        });

        IdeManager.getInstance().registerTaskToRun(project, rawAndSourceCombinerTask);

        configuration.getDependencies().add(entry.asDependency(project));
    }

}
