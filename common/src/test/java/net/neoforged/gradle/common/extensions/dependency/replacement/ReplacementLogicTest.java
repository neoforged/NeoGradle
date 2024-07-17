package net.neoforged.gradle.common.extensions.dependency.replacement;

import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.tasks.DependencyGenerationTask;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementResult;
import net.neoforged.gradle.dsl.common.extensions.repository.Entry;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unused", "unchecked"})
public class ReplacementLogicTest {


    @Test
    public void aConfigureCallbackForAllConfigurationsInAGivenProjectIsAddedOnConstruction() {
        final Project project = mock(Project.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);

        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);

        final ReplacementLogic dependencyReplacementsExtension = new SystemUnderTest(project);

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


        when(project.getConfigurations()).thenReturn(configurationContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(project.getObjects()).thenReturn(objectFactory);
        when(configuration.getDependencies()).thenReturn(dependencySet);
        doAnswer(invocation -> {
            final Action<Configuration> configurationAction = invocation.getArgument(0);
            configurationAction.execute(configuration);
            return null;
        }).when(configurationContainer).configureEach(any());

        final ReplacementLogic dependencyReplacementsExtension = new SystemUnderTest(project);

        verify(configuration).withDependencies(any());
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
        final Repository repository = mock(Repository.class);

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

        final ReplacementLogic dependencyReplacementsExtension = new SystemUnderTest(project);

        Assertions.assertThrows(IllegalStateException.class, () -> dependencyReplacementsExtension.createDummyDependency(mock(ProjectDependency.class), mock(ReplacementResult.class)));

        verify(repository, never()).withEntry(any());
    }

    @Test
    public void callingHandleConfigurationRegistersDependencyMonitor() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final ObjectFactory objectFactory = mock(ObjectFactory.class);
        final ConfigurationContainer configurationContainer = mock(ConfigurationContainer.class);
        final Configuration configuration = mock(Configuration.class);
        final DependencySet dependencySet = mock(DependencySet.class);
        final Repository repository = mock(Repository.class);
        final IdeManagementExtension ideManagementExtension = mock(IdeManagementExtension.class);
        final DependencyGenerationTask dependencyGeneratorTaskInstance = mock(DependencyGenerationTask.class);

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

        when(repository.withEntry(any())).thenReturn(mock(Entry.class));

        final ReplacementLogic dependencyReplacementsExtension = new SystemUnderTest(project);

        verify(dependencySet).configureEach(any());
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
        final Repository repository = mock(Repository.class);

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

        when(repository.withEntry(any())).thenReturn(mock(Entry.class));

        final ReplacementLogic dependencyReplacementsExtension = new SystemUnderTest(project);

        dependencyReplacementsExtension.createDummyDependency(mock(ExternalModuleDependency.class), mock(ReplacementResult.class));

        verify(repository).withEntry(any());
    }

    private static class SystemUnderTest extends ReplacementLogic {

        private final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);

        public SystemUnderTest(Project project) {
            super(project);
        }

        @Override
        public ExtensionContainer getExtensions() {
            return extensionContainer;
        }
    }

}
