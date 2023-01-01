package net.minecraftforge.gradle.common.extensions.dependency.replacement;

import com.google.common.collect.Sets;
import net.minecraftforge.gradle.common.extensions.repository.IvyDummyRepositoryExtension;
import net.minecraftforge.gradle.common.ide.IdeManager;
import net.minecraftforge.gradle.common.extensions.repository.IvyDummyRepositoryEntry;
import net.minecraftforge.gradle.common.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.common.tasks.DependencyGenerationTask;
import net.minecraftforge.gradle.common.tasks.RawAndSourceCombiner;
import net.minecraftforge.gradle.common.util.ConfigurableNamedDSLObjectContainer;
import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.dsl.base.util.NamedDSLObjectContainer;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.dsl.common.extensions.repository.Repository;
import net.minecraftforge.gradle.dsl.common.extensions.repository.RepositoryEntry;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public abstract class DependencyReplacementsExtension extends ConfigurableObject<DependencyReplacement> implements DependencyReplacement {

    private final Project project;
    private final TaskProvider<? extends DependencyGenerationTask> dependencyGenerator;
    private final Set<Configuration> configuredConfigurations = Sets.newHashSet();
    private final ConfigurableNamedDSLObjectContainer.Simple<DependencyReplacementHandler> dependencyReplacementHandlers;
    private boolean registeredTaskToIde;

    @Inject
    public DependencyReplacementsExtension(Project project) {
        this.project = project;
        this.project.getConfigurations().configureEach(configuration -> configuration.getDependencies().whenObjectAdded(dependency -> {
            if (dependency instanceof ModuleDependency) {
                final ModuleDependency moduleDependency = (ModuleDependency) dependency;
                handleDependency(configuration, moduleDependency);
            }
        }));

        this.dependencyGenerator = this.project.getTasks().register("generateDependencies", DependencyGenerationTask.class);
        this.dependencyReplacementHandlers = this.project.getObjects().newInstance(
                ConfigurableNamedDSLObjectContainer.Simple.class,
                this.project,
                DependencyReplacementHandler.class,
                (NamedDomainObjectFactory<DependencyReplacementHandler>) name -> getProject().getObjects().newInstance(DependencyReplacementHandlerImpl.class, getProject(), name)
        );
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    @NotNull
    public NamedDSLObjectContainer<?, DependencyReplacementHandler> getReplacementHandlers() {
        return this.dependencyReplacementHandlers;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") //Two lines above the get....
    private void handleDependency(final Configuration configuration, final ModuleDependency dependency) {
        Set<DependencyReplacementHandler> replacementHandlers = getReplacementHandlers();
        replacementHandlers.stream()
                .map(handler -> handler.getReplacer().get() .get(new DependencyReplacementContext(project, configuration, dependency)))
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get)
                .ifPresent(result -> handleDependencyReplacement(configuration, dependency, result));
    }

    private void handleDependencyReplacement(Configuration configuration, Dependency dependency, DependencyReplacementResult result) {
        configuration.getDependencies().remove(dependency);

        registerDependencyProviderTaskIfNecessaryTo(configuration);

        if (IdeManager.getInstance().isIdeImportInProgress()) {
            handleDependencyReplacementForIde(configuration, dependency, result);
        }

        handleDependencyReplacementForGradle(configuration, dependency, result);
    }

    private void registerDependencyProviderTaskIfNecessaryTo(Configuration configuration) {
        if (!this.configuredConfigurations.contains(configuration)) {
            this.configuredConfigurations.add(configuration);
            configuration.getDependencies().add(this.project.getDependencies().create(this.project.files(this.dependencyGenerator.get())));
        }

        if (!registeredTaskToIde) {
            IdeManager.getInstance().registerTaskToRun(project, dependencyGenerator);
            registeredTaskToIde = true;
        }
    }

    private void handleDependencyReplacementForGradle(final Configuration configuration, final Dependency dependency, final DependencyReplacementResult result) {
        createDependencyReplacementResult(configuration, dependency, result, (repoBaseDir, entry) -> {
            final String artifactSelectionTaskName = result.getTaskNameBuilder().apply("selectRawArtifact");
            return project.getTasks().register(artifactSelectionTaskName, ArtifactFromOutput.class, artifactFromOutput -> {
                artifactFromOutput.setGroup("forgegradle/dependencies");
                artifactFromOutput.setDescription(String.format("Selects the raw artifact from the %s dependency and puts it in the Ivy repository", dependency));

                artifactFromOutput.getInput().set(result.getRawJarTaskProvider().flatMap(WithOutput::getOutput));
                artifactFromOutput.getOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.buildArtifactPath(dir.getAsFile().toPath()).toFile())));
                artifactFromOutput.dependsOn(result.getRawJarTaskProvider());
            });
        });
    }

    private void handleDependencyReplacementForIde(final Configuration configuration, final Dependency dependency, final DependencyReplacementResult result) {
        createDependencyReplacementResult(configuration, dependency, result, ((repoBaseDir, entry) -> {
            final String dependencyExporterTaskName = result.getTaskNameBuilder().apply("combined");
            return project.getTasks().register(dependencyExporterTaskName, RawAndSourceCombiner.class, rawAndSourceCombiner -> {
                rawAndSourceCombiner.setGroup("forgegradle/dependencies");
                rawAndSourceCombiner.setDescription(String.format("Combines the raw and sources jars into a single task execution tree for: %s", dependency.toString()));

                rawAndSourceCombiner.getRawJarInput().set(result.getRawJarTaskProvider().flatMap(WithOutput::getOutput));
                rawAndSourceCombiner.getSourceJarInput().set(result.getSourcesJarTaskProvider().flatMap(WithOutput::getOutput));

                rawAndSourceCombiner.getRawJarOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.buildArtifactPath(dir.getAsFile().toPath()).toFile())));
                rawAndSourceCombiner.getSourceJarOutput().fileProvider(repoBaseDir.map(TransformerUtils.guard(dir -> entry.asSources().buildArtifactPath(dir.getAsFile().toPath()).toFile())));
            });
        }));
    }

    private void createDependencyReplacementResult(final Configuration configuration, final Dependency dependency, final DependencyReplacementResult result, final TaskProviderGenerator generator) {
        if (!(dependency instanceof ExternalModuleDependency)) {
            return;
        }

        final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependency;

        final Repository<?,?,?,?,?> extension = project.getExtensions().getByType(Repository.class);
        final Provider<Directory> repoBaseDir = extension.getRepositoryDirectory();
        try {
            extension.withDependency(builder -> {
                builder.from(externalModuleDependency);
                result.getDependencyMetadataConfigurator().accept(builder);

                result.getAdditionalDependenciesConfiguration().getDependencies()
                        .stream()
                        .filter(ExternalModuleDependency.class::isInstance)
                        .map(ExternalModuleDependency.class::cast)
                        .forEach(additionalDependency -> builder.withDependency(depBuilder -> depBuilder.from(additionalDependency)));
            }, entry -> {
                final TaskProvider<? extends Task> rawAndSourceCombinerTask = generator.generate(repoBaseDir, entry);

                final Dependency replacedDependency = entry.toGradle(project);
                configuration.getDependencies().add(replacedDependency);
                result.getOnCreateReplacedDependencyCallback().accept(replacedDependency);

                this.dependencyGenerator.configure(task -> task.dependsOn(rawAndSourceCombinerTask));
            });
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException(String.format("Failed to create the dummy dependency for: %s", dependency), e);
        }
    }

    @FunctionalInterface
    private interface TaskProviderGenerator {

        TaskProvider<? extends Task> generate(final Provider<Directory> repoBaseDir, final RepositoryEntry<?,?> entry);
    }

}
