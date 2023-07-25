package net.neoforged.gradle.common.extensions.dependency.replacement;

import net.neoforged.gradle.common.dummy.DummyRepositoryDependency;
import net.neoforged.gradle.common.dummy.DummyRepositoryEntry;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.extensions.dependency.creation.DependencyCreator;
import net.neoforged.gradle.common.tasks.DependencyGenerationTask;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacer;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryEntry;
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryReference;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.ProjectState;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class DependencyReplacementsExtensionTest {


    @Test
    public void aConfigureCallbackForAllConfigurationsInAGivenProjectIsAddedOnConstruction() {
        final Project project = mock(Project.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        verify(configurationContainer).configureEach(any());
    }

    @Test
    public void aCallbackHandlerIsAddedToEachConfigurationWhenOneIsConfigured() {
        final Project project = mock(Project.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        verify(dependencySet).whenObjectAdded(ArgumentMatchers.<Action<? super Dependency>>any());
    }

    @Test
    public void aDependencyGenerationTaskIsAddedToTheProjectOnConstruction() {
        final Project project = mock(Project.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        verify(taskContainer).register(eq("generateDependencies"), ArgumentMatchers.<Class<? extends Task>>any());
    }

    @Test
    public void invokingOnPostDefinitionBakesRunsTheRegisteredCallbacksIfTheStateHasNoFailure() {
        final Project project = mock(Project.class);
        final ProjectState projectState = mock(ProjectState.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        when(project.getState()).thenReturn(projectState);
        when(projectState.getFailure()).thenReturn(null);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        final AtomicBoolean hasRun = new AtomicBoolean(false);
        dependencyReplacementsExtension.getAfterDefinitionBakeCallbacks().add((p) -> hasRun.set(true));

        assertFalse(dependencyReplacementsExtension.hasBeenBaked());
        assertFalse(hasRun.get());

        dependencyReplacementsExtension.onPostDefinitionBakes(project);

        assertTrue(dependencyReplacementsExtension.hasBeenBaked());
        assertTrue(hasRun.get());
    }

    @Test
    public void invokingOnPostDefinitionBakesDoesNotRunTheRegisteredCallbacksIfTheStateHasAFailure() {
        final Project project = mock(Project.class);
        final ProjectState projectState = mock(ProjectState.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        when(project.getState()).thenReturn(projectState);
        when(projectState.getFailure()).thenReturn(new Exception());
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        final AtomicBoolean hasRun = new AtomicBoolean(false);
        dependencyReplacementsExtension.getAfterDefinitionBakeCallbacks().add((p) -> hasRun.set(true));

        assertFalse(dependencyReplacementsExtension.hasBeenBaked());
        assertFalse(hasRun.get());

        dependencyReplacementsExtension.onPostDefinitionBakes(project);

        assertTrue(dependencyReplacementsExtension.hasBeenBaked());
        assertFalse(hasRun.get());
    }

    @Test
    public void aDependencyReplacementIsOnlyRegisteredForAnythingThatIsNotAnExternalModuleDependency() throws XMLStreamException, IOException {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        dependencyReplacementsExtension.createDependencyReplacementResult(
                configuration,
                mock(ProjectDependency.class),
                mock(DependencyReplacementResult.class),
                mock(DependencyReplacementsExtension.TaskProviderGenerator.class)
        );

        verify(repository, never()).withDependency(any(), any(), any(), any());
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Test
    public void callingHandleDependencyReplacementRemovesTheOriginalDependency() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final IdeManagementExtension ideManagementExtension = mock(IdeManagementExtension.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);
        final TaskProvider<? extends Task> dependencyGeneratorTask = mock(TaskProvider.class);
        final DependencyGenerationTask dependencyGeneratorTaskInstance = mock(DependencyGenerationTask.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(extensionContainer.getByType(IdeManagementExtension.class)).thenReturn(ideManagementExtension);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        when(taskContainer.register(eq("generateDependencies"), eq(DependencyGenerationTask.class))).thenAnswer(invocation -> dependencyGeneratorTask);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());
        when(dependencyGeneratorTask.get()).thenAnswer(invocation -> dependencyGeneratorTaskInstance);

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        dependencyReplacementsExtension.handleDependencyReplacement(
                configuration,
                mock(ProjectDependency.class),
                mock(DependencyReplacementResult.class),
                mock(DependencyReplacementsExtension.DependencyReplacer.class),
                mock(DependencyReplacementsExtension.DependencyReplacer.class)
        );

        verify(dependencySet).remove(any());
    }

    @Test
    public void callingHandleDependencyReplacementToRegistersTheGenerationTaskWhenNotDoneYet() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final IdeManagementExtension ideManagementExtension = mock(IdeManagementExtension.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);
        final TaskProvider<? extends Task> dependencyGeneratorTask = mock(TaskProvider.class);
        final DependencyGenerationTask dependencyGeneratorTaskInstance = mock(DependencyGenerationTask.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(extensionContainer.getByType(IdeManagementExtension.class)).thenReturn(ideManagementExtension);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        when(taskContainer.register(eq("generateDependencies"), eq(DependencyGenerationTask.class))).thenAnswer(invocation -> dependencyGeneratorTask);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());
        when(dependencyGeneratorTask.get()).thenAnswer(invocation -> dependencyGeneratorTaskInstance);

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        dependencyReplacementsExtension.handleDependencyReplacement(
                configuration,
                mock(ProjectDependency.class),
                mock(DependencyReplacementResult.class),
                mock(DependencyReplacementsExtension.DependencyReplacer.class),
                mock(DependencyReplacementsExtension.DependencyReplacer.class)
        );

        verify(dependencySet).add(any());
    }

    @Test
    public void callingHandleDependencyReplacementDoesNotRegisterTheGenerationTaskWhenAlreadyDone() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final IdeManagementExtension ideManagementExtension = mock(IdeManagementExtension.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(extensionContainer.getByType(IdeManagementExtension.class)).thenReturn(ideManagementExtension);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);
        dependencyReplacementsExtension.getConfiguredConfigurations().add(configuration);

        dependencyReplacementsExtension.handleDependencyReplacement(
                configuration,
                mock(ProjectDependency.class),
                mock(DependencyReplacementResult.class),
                mock(DependencyReplacementsExtension.DependencyReplacer.class),
                mock(DependencyReplacementsExtension.DependencyReplacer.class)
        );

        verify(dependencySet, never()).add(any());
    }

    @Test
    public void callingRegisterDependencyProviderTaskIfNecessaryToRegistersTheGenerationTaskWhenNotDoneYet() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final IdeManagementExtension ideManagementExtension = mock(IdeManagementExtension.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);
        final TaskProvider<? extends Task> dependencyGeneratorTask = mock(TaskProvider.class);
        final DependencyGenerationTask dependencyGeneratorTaskInstance = mock(DependencyGenerationTask.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(extensionContainer.getByType(IdeManagementExtension.class)).thenReturn(ideManagementExtension);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        when(taskContainer.register(eq("generateDependencies"), eq(DependencyGenerationTask.class))).thenAnswer(invocation -> dependencyGeneratorTask);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());
        when(dependencyGeneratorTask.get()).thenAnswer(invocation -> dependencyGeneratorTaskInstance);

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        dependencyReplacementsExtension.registerDependencyProviderTaskIfNecessaryTo(
                configuration
        );

        verify(dependencySet).add(any());
    }

    @Test
    public void callingRegisterDependencyProviderTaskIfNecessaryToDoesNotRegisterTheGenerationTaskWhenAlreadyDone() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final IdeManagementExtension ideManagementExtension = mock(IdeManagementExtension.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(extensionContainer.getByType(IdeManagementExtension.class)).thenReturn(ideManagementExtension);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);
        dependencyReplacementsExtension.getConfiguredConfigurations().add(configuration);

        dependencyReplacementsExtension.registerDependencyProviderTaskIfNecessaryTo(
                configuration
        );

        verify(dependencySet, never()).add(any());
    }

    @Test
    public void callingHandleDependencyReplacementAlwaysInvokesTheGradleReplacementHandler() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final IdeManagementExtension ideManagementExtension = mock(IdeManagementExtension.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);
        final DependencyReplacementsExtension.DependencyReplacer gradleReplacementHandler = mock(DependencyReplacementsExtension.DependencyReplacer.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(extensionContainer.getByType(IdeManagementExtension.class)).thenReturn(ideManagementExtension);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);
        dependencyReplacementsExtension.getConfiguredConfigurations().add(configuration);

        dependencyReplacementsExtension.handleDependencyReplacement(
                configuration,
                mock(ProjectDependency.class),
                mock(DependencyReplacementResult.class),
                mock(DependencyReplacementsExtension.DependencyReplacer.class),
                gradleReplacementHandler
        );

        verify(gradleReplacementHandler).handle(any(), any(), any());
    }

    @Test
    public void callingHandleDependencyReplacementAlwaysInvokesTheIdeReplacementHandlerWhenAnImportIsRunning() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final IdeManagementExtension ideManagementExtension = mock(IdeManagementExtension.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);
        final DependencyReplacementsExtension.DependencyReplacer ideReplacementHandler = mock(DependencyReplacementsExtension.DependencyReplacer.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(extensionContainer.getByType(IdeManagementExtension.class)).thenReturn(ideManagementExtension);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());
        when(ideManagementExtension.isIdeImportInProgress()).thenReturn(true);

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);
        dependencyReplacementsExtension.getConfiguredConfigurations().add(configuration);

        dependencyReplacementsExtension.handleDependencyReplacement(
                configuration,
                mock(ProjectDependency.class),
                mock(DependencyReplacementResult.class),
                ideReplacementHandler,
                mock(DependencyReplacementsExtension.DependencyReplacer.class)
        );

        verify(ideReplacementHandler).handle(any(), any(), any());
    }

    @Test
    public void callingHandleDependencyReplacementAlwaysInvokesTheIdeReplacementHandlerWhenNoImportIsRunning() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final IdeManagementExtension ideManagementExtension = mock(IdeManagementExtension.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);
        final DependencyReplacementsExtension.DependencyReplacer ideReplacementHandler = mock(DependencyReplacementsExtension.DependencyReplacer.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(extensionContainer.getByType(IdeManagementExtension.class)).thenReturn(ideManagementExtension);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());
        when(ideManagementExtension.isIdeImportInProgress()).thenReturn(false);

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);
        dependencyReplacementsExtension.getConfiguredConfigurations().add(configuration);

        dependencyReplacementsExtension.handleDependencyReplacement(
                configuration,
                mock(ProjectDependency.class),
                mock(DependencyReplacementResult.class),
                ideReplacementHandler,
                mock(DependencyReplacementsExtension.DependencyReplacer.class)
        );

        verify(ideReplacementHandler, never()).handle(any(), any(), any());
    }

    @Test
    public void aDependencyReplacementIsRegisteredForAnExternalModuleDependency() throws XMLStreamException, IOException {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        dependencyReplacementsExtension.createDependencyReplacementResult(
                configuration,
                mock(ExternalModuleDependency.class),
                mock(DependencyReplacementResult.class),
                mock(DependencyReplacementsExtension.TaskProviderGenerator.class)
        );

        verify(repository).withDependency(any(), any(), any(), any());
    }

    @Test
    public void aDependencyReplacementProperlyConfiguresTheIvyReplacement() throws XMLStreamException, IOException {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        final DependencyArtifact artifact = mock(DependencyArtifact.class);
        final DependencyReplacementResult result = mock(DependencyReplacementResult.class);
        final Configuration additionalDependencies = mock(Configuration.class);
        final DependencySet additionalDependenciesSet = mock(DependencySet.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        final Collection<Action<RepositoryEntry.Builder<?, ?, ?>>> actions = new ArrayList<>();

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());
        doAnswer(invocation -> {
            Action<RepositoryEntry.Builder<?, ?, ?>> configurator = invocation.getArgument(0);
            actions.add(configurator);
            return null;
        }).when(repository).withDependency(any(), any());
        when(dependency.getGroup()).thenReturn("group");
        when(dependency.getName()).thenReturn("name");
        when(dependency.getVersion()).thenReturn("version");
        when(artifact.getName()).thenReturn("artifactName");
        when(artifact.getType()).thenReturn("artifactType");
        when(artifact.getClassifier()).thenReturn("artifactClassifier");
        when(artifact.getExtension()).thenReturn("artifactExtension");
        when(dependency.getArtifacts()).thenReturn(Collections.singleton(artifact));
        when(result.getDependencyMetadataConfigurator()).thenReturn((b) -> {});
        when(result.getAdditionalDependenciesConfiguration()).thenReturn(additionalDependencies);
        when(additionalDependencies.getDependencies()).thenReturn(additionalDependenciesSet);
        when(additionalDependenciesSet.stream()).thenReturn(Stream.empty());

        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        dependencyReplacementsExtension.createDependencyReplacementResult(
                configuration,
                dependency,
                result,
                mock(DependencyReplacementsExtension.TaskProviderGenerator.class)
        );

        assertFalse(actions.isEmpty());
        assertEquals(1, actions.size());

        final DummyRepositoryEntry entry = new DummyRepositoryEntry(project);
        actions.iterator().next().execute(entry);

        assertEquals("group", entry.getGroup());
        assertEquals("name", entry.getName());
        assertEquals("version", entry.getVersion());
        assertEquals("artifactClassifier", entry.getClassifier());
        assertEquals("artifactExtension", entry.getExtension());

        assertTrue(entry.getDependencies().isEmpty());
    }

    @Test
    public void aDependencyReplacementProperlyConfiguresTheIvyReplacementWithItsOwnExternalModuleDependencies() throws XMLStreamException, IOException {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository<?, ?, ?, ?, ?> repository = mock(Repository.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        final DependencyArtifact artifact = mock(DependencyArtifact.class);
        final DependencyReplacementResult result = mock(DependencyReplacementResult.class);
        final Configuration additionalDependencies = mock(Configuration.class);
        final DependencySet additionalDependenciesSet = mock(DependencySet.class);
        final ExternalModuleDependency additionalDependency = mock(ExternalModuleDependency.class);
        final DependencyCreator dependencyCreator = mock(DependencyCreator.class);

        final Collection<Action<RepositoryEntry.Builder<?, ?, ?>>> actions = new ArrayList<>();

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(Repository.class)).thenReturn(repository);
        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());
        doAnswer(invocation -> {
            Action<RepositoryEntry.Builder<?, ?, ?>> configurator = invocation.getArgument(0);
            actions.add(configurator);
            return null;
        }).when(repository).withDependency(any(), any());
        when(dependency.getGroup()).thenReturn("group");
        when(dependency.getName()).thenReturn("name");
        when(dependency.getVersion()).thenReturn("version");
        when(artifact.getName()).thenReturn("artifactName");
        when(artifact.getType()).thenReturn("artifactType");
        when(artifact.getClassifier()).thenReturn("artifactClassifier");
        when(artifact.getExtension()).thenReturn("artifactExtension");
        when(dependency.getArtifacts()).thenReturn(Collections.singleton(artifact));
        when(result.getDependencyMetadataConfigurator()).thenReturn((b) -> {});
        when(result.getAdditionalDependenciesConfiguration()).thenReturn(additionalDependencies);
        when(additionalDependencies.getDependencies()).thenReturn(additionalDependenciesSet);
        when(additionalDependenciesSet.stream()).thenReturn(Stream.of(additionalDependency));
        when(additionalDependency.getGroup()).thenReturn("additionalGroup");
        when(additionalDependency.getName()).thenReturn("additionalName");
        when(additionalDependency.getVersion()).thenReturn("additionalVersion");
        when(additionalDependency.getArtifacts()).thenReturn(Collections.emptySet());


        final DependencyReplacementsExtension dependencyReplacementsExtension = new SystemUnderTest(project, dependencyCreator);

        dependencyReplacementsExtension.createDependencyReplacementResult(
                configuration,
                dependency,
                result,
                mock(DependencyReplacementsExtension.TaskProviderGenerator.class)
        );

        assertFalse(actions.isEmpty());
        assertEquals(1, actions.size());

        final DummyRepositoryEntry entry = new DummyRepositoryEntry(project);
        actions.iterator().next().execute(entry);

        assertEquals("group", entry.getGroup());
        assertEquals("name", entry.getName());
        assertEquals("version", entry.getVersion());
        assertEquals("artifactClassifier", entry.getClassifier());
        assertEquals("artifactExtension", entry.getExtension());

        assertFalse(entry.getDependencies().isEmpty());
        assertEquals(1, entry.getDependencies().size());
        final RepositoryReference dummyRepositoryDependency = entry.getDependencies().iterator().next();
        assertEquals("additionalGroup", dummyRepositoryDependency.getGroup());
        assertEquals("additionalName", dummyRepositoryDependency.getName());
        assertEquals("additionalVersion", dummyRepositoryDependency.getVersion());
        assertNull(dummyRepositoryDependency.getClassifier());
        assertNull(dummyRepositoryDependency.getExtension());
    }


    private static class SystemUnderTest extends DependencyReplacementsExtension {

        private final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);

        public SystemUnderTest(Project project, DependencyCreator creator) {
            super(project, creator);
        }

        @Override
        public ExtensionContainer getExtensions() {
            return extensionContainer;
        }
    }

}