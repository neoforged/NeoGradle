package net.neoforged.gradle.common.util;

import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class ConfigurationUtilsTest {

    @Test
    public void findCompileClasspathSourceSetHandlesImplementationAndCompileClasspath() {
        final Configuration compileClasspath = mock(Configuration.class);
        final Configuration compileOnly = mock(Configuration.class);
        final Configuration implementation = mock(Configuration.class);

        final ConfigurationContainer configurations = mock(ConfigurationContainer.class);
        final Project project = mock(Project.class);
        final ExtensionContainer extensions = mock(ExtensionContainer.class);

        final SourceSetContainer sourceSets = mock(SourceSetContainer.class);
        final SourceSet mainSourceSet = mock(SourceSet.class);

        when(configurations.findByName("compileOnly")).thenReturn(compileOnly);
        when(configurations.findByName("compileClasspath")).thenReturn(compileClasspath);
        when(configurations.findByName("implementation")).thenReturn(implementation);

        when(project.getConfigurations()).thenReturn(configurations);

        when(project.getExtensions()).thenReturn(extensions);
        when(extensions.getByType(SourceSetContainer.class)).thenReturn(sourceSets);

        when(sourceSets.getByName("main")).thenReturn(mainSourceSet);
        when(mainSourceSet.getCompileClasspathConfigurationName()).thenReturn("compileClasspath");
        when(mainSourceSet.getImplementationConfigurationName()).thenReturn("implementation");
        when(mainSourceSet.getCompileOnlyConfigurationName()).thenReturn("compileOnly");
        doAnswer(invocationOnMock -> {
            final Consumer<SourceSet> argument = invocationOnMock.getArgument(0);
            argument.accept(mainSourceSet);
            return null;
        }).when(sourceSets).forEach(ArgumentMatchers.any());

        when(compileOnly.getName()).thenReturn("compileOnly");
        when(compileClasspath.getName()).thenReturn("compileClasspath");
        when(implementation.getName()).thenReturn("implementation");

        when(compileClasspath.getExtendsFrom()).thenReturn(buildConfigurationSet(implementation, compileOnly));

        final List<Configuration> result = ConfigurationUtils.findCompileOnlyConfigurationForSourceSetReplacement(project, implementation);

        Assertions.assertEquals(buildConfigurationList(compileOnly), result);
    }

    @Test
    public void findRuntimeClasspathSourceSetHandlesImplementationAndRuntimeClasspath() {
        final Configuration runtimeClasspath = mock(Configuration.class);
        final Configuration implementation = mock(Configuration.class);

        final ConfigurationContainer configurations = mock(ConfigurationContainer.class);
        final Project project = mock(Project.class);
        final ExtensionContainer extensions = mock(ExtensionContainer.class);

        final SourceSetContainer sourceSets = mock(SourceSetContainer.class);
        final SourceSet mainSourceSet = mock(SourceSet.class);

        when(configurations.findByName("runtimeClasspath")).thenReturn(runtimeClasspath);
        when(configurations.findByName("implementation")).thenReturn(implementation);

        final List<Configuration> newConfigurations = new ArrayList<>();
        when(configurations.maybeCreate(any())).thenAnswer((Answer<Configuration>) invocationOnMock -> {
            final String name = invocationOnMock.getArgument(0);
            final Configuration configuration = mock(Configuration.class);
            when(configuration.getName()).thenReturn(name);

            newConfigurations.add(configuration);

            return configuration;
        });

        when(project.getConfigurations()).thenReturn(configurations);

        when(project.getExtensions()).thenReturn(extensions);
        when(extensions.getByType(SourceSetContainer.class)).thenReturn(sourceSets);

        when(sourceSets.getByName("main")).thenReturn(mainSourceSet);
        when(mainSourceSet.getRuntimeClasspathConfigurationName()).thenReturn("runtimeClasspath");
        when(mainSourceSet.getImplementationConfigurationName()).thenReturn("implementation");

        doAnswer(invocationOnMock -> {
            final Consumer<SourceSet> argument = invocationOnMock.getArgument(0);
            argument.accept(mainSourceSet);
            return null;
        }).when(sourceSets).forEach(ArgumentMatchers.any());

        when(runtimeClasspath.getName()).thenReturn("runtimeClasspath");
        when(implementation.getName()).thenReturn("implementation");

        when(runtimeClasspath.getExtendsFrom()).thenReturn(buildConfigurationSet(implementation));

        final List<Configuration> result = ConfigurationUtils.findRuntimeOnlyConfigurationFromSourceSetReplacement(project, implementation);

        Assertions.assertEquals(newConfigurations, result);
    }

    @Test
    public void getAllExtendingConfigurationOneDeepOnSuperLookup() {
        final Configuration source = mock(Configuration.class);
        final Configuration target = mock(Configuration.class);

        when(source.getExtendsFrom()).thenReturn(buildConfigurationSet(target));

        final Set<Configuration> configurations = ConfigurationUtils.getAllSuperConfigurations(source);

        Assertions.assertEquals(buildConfigurationSet(target), configurations);
    }

    @Test
    public void getAllExtendingConfigurationRecursiveOnSuperLookup() {
        final Configuration source = mock(Configuration.class);
        final Configuration levelOne = mock(Configuration.class);
        final Configuration levelTwo = mock(Configuration.class);

        when(source.getExtendsFrom()).thenReturn(buildConfigurationSet(levelOne));
        when(levelOne.getExtendsFrom()).thenReturn(buildConfigurationSet(levelTwo));

        final Set<Configuration> configurations = ConfigurationUtils.getAllSuperConfigurations(source);

        Assertions.assertEquals(buildConfigurationSet(levelOne, levelTwo), configurations);
    }

    private Set<Configuration> buildConfigurationSet(Configuration... configurations) {
        final Set<Configuration> configurationSet = new HashSet<>();
        Collections.addAll(configurationSet, configurations);
        return configurationSet;
    }

    private List<Configuration> buildConfigurationList(Configuration... configurations) {
        final List<Configuration> configurationSet = new ArrayList<>();
        Collections.addAll(configurationSet, configurations);
        return configurationSet;
    }

}
