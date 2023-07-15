package net.neoforged.gradle.common.deobfuscation;

import com.google.common.collect.Sets;
import net.neoforged.gradle.common.dummy.DummyRepositoryDependency;
import net.neoforged.gradle.common.dummy.DummyRepositoryEntry;
import net.neoforged.gradle.common.extensions.ForcedDependencyDeobfuscationExtension;
import net.neoforged.gradle.common.extensions.dependency.replacement.DependencyReplacementsExtension;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.Context;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacer;
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryReference;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.minecraftforge.trainingwheels.base.file.FileTestingUtils;
import net.minecraftforge.trainingwheels.base.file.PathFile;
import net.minecraftforge.trainingwheels.gradle.base.task.TaskMockingUtils;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.LenientConfiguration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskContainer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DependencyDeobfuscatorTest {

    @Test
    public void applyingTheExtensionCreatesADependencyReplacementHandler() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);

        final DependencyDeobfuscator target = new DependencyDeobfuscator(project) {};

        verify(handlers, times(1)).create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class));
    }

    @Test
    public void weDoNotDeobfuscateADependencyThatIsNotAnExternalModule() {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        final DependencyDeobfuscator target = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ProjectDependency projectDependency = mock(ProjectDependency.class);

        when(context.getDependency()).thenReturn(projectDependency);

        //Now validate that the replacer returns an empty optional
        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDoNotDeobfuscateADependencyThatDoesNotExist() {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any())).thenReturn(detachedConfiguration);
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.emptySet());

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        final DependencyDeobfuscator target = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);

        //Now validate that the replacer returns an empty optional
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDoNotDeobfuscateADependencyWithoutFiles() {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any())).thenReturn(detachedConfiguration);
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.emptySet());

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        final DependencyDeobfuscator target = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);

        //Now validate that the replacer returns an empty optional
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDoNotDeobfuscateADependencyWithMultipleFiles() {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any())).thenReturn(detachedConfiguration);
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Sets.newHashSet(mock(ResolvedDependency.class), mock(ResolvedDependency.class)));

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        final DependencyDeobfuscator target = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDoNotDeobfuscateADependencyWithoutAnArtifact() {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any())).thenReturn(detachedConfiguration);
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.emptySet());

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        final DependencyDeobfuscator target = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDoNotDeobfuscateADependencyWithMultipleArtifacts() {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any())).thenReturn(detachedConfiguration);
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Sets.newHashSet(mock(ResolvedArtifact.class), mock(ResolvedArtifact.class)));

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        final DependencyDeobfuscator target = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDoNotDeobfuscateADependencyWithAMissingFile() {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        final PathFile target = FileTestingUtils.newSimpleTestFile("does-not-exist");

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any())).thenReturn(detachedConfiguration);
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));
        when(resolvedArtifact.getFile()).thenReturn(target);

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        final DependencyDeobfuscator obfuscator = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDoNotDeobfuscateADependencyWithANoneJarFile() throws IOException {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        final PathFile target = FileTestingUtils.newSimpleTestFile("not-a-jar");

        Files.write(target.toPath(), "not a jar".getBytes(StandardCharsets.UTF_8));

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any())).thenReturn(detachedConfiguration);
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));
        when(resolvedArtifact.getFile()).thenReturn(target);

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        final DependencyDeobfuscator obfuscator = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDoNotDeobfuscateADependencyWithAJarWithoutAManifest() throws IOException {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        final PathFile target = FileTestingUtils.newSimpleTestFile("some.jar");

        final ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target.toPath()));
        zip.flush();
        zip.close();

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any())).thenReturn(detachedConfiguration);
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));
        when(resolvedArtifact.getFile()).thenReturn(target);

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        final DependencyDeobfuscator obfuscator = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDoNotDeobfuscateADependencyWithAJarWithoutTheProperFieldsInTheManifest() throws IOException {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        final PathFile target = FileTestingUtils.newSimpleTestFile("some.jar");

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any())).thenReturn(detachedConfiguration);
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));
        when(resolvedArtifact.getFile()).thenReturn(target);

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        createSimpleJar(target);
        final DependencyDeobfuscator obfuscator = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertFalse(result.isPresent());
    }

    @Test
    public void weDeobfuscateADependencyWithASimpleJarWithoutDependencies() throws IOException {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        final PathFile target = FileTestingUtils.newSimpleTestFile("some.jar");
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final List<Task> tasks = new ArrayList<>();
        final Mappings mappings = mock(Mappings.class);
        final Property<NamingChannel> namingChannelProperty = mock(Property.class);
        final NamingChannel namingChannel = mock(NamingChannel.class);
        final Property<String> deobfuscationGroupSupplier = mock(Property.class);


        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(extensionContainer.getByType(Mappings.class)).thenReturn(mappings);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any()))
                .thenReturn(detachedConfiguration)
                .then((Answer<Configuration>) invocation -> {
                    final Dependency[] dependenciesIn = invocation.getArguments().length > 0 ? invocation.getArgument(0) : new Dependency[0];
                    final Configuration configuration = mock(Configuration.class);
                    final DependencySet dependencies = mock(DependencySet.class);
                    when(configuration.getDependencies()).thenReturn(dependencies);
                    when(configuration.getFiles()).thenThrow(new RuntimeException("Tried to resolve a detached configuration that was mocked for a situation where that should not happen."));
                    when(dependencies.isEmpty()).thenReturn(dependenciesIn.length == 0);
                    return configuration;
                });
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));
        when(resolvedArtifact.getFile()).thenReturn(target);
        when(resolvedDependency.getChildren()).thenReturn(Collections.emptySet());
        when(resolvedDependency.getName()).thenReturn("Dummy");
        when(resolvedDependency.getModuleGroup()).thenReturn("group");
        when(resolvedDependency.getModuleName()).thenReturn("name");
        when(resolvedDependency.getModuleVersion()).thenReturn("version");
        when(resolvedArtifact.getClassifier()).thenReturn("classifier");
        when(resolvedArtifact.getExtension()).thenReturn("extension");
        when(mappings.getChannel()).thenReturn(namingChannelProperty);
        when(namingChannelProperty.get()).thenReturn(namingChannel);
        when(namingChannel.getDeobfuscationGroupSupplier()).thenReturn(deobfuscationGroupSupplier);
        when(namingChannel.getName()).thenReturn("naming_channel");
        when(deobfuscationGroupSupplier.get()).thenReturn("");

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        when(taskContainer.register(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Class.class), ArgumentMatchers.any(Action.class))).thenAnswer(invocation -> {
            final String name = invocation.getArgument(0);
            final Class<? extends Task> type = invocation.getArgument(1);
            final Action action = invocation.getArgument(2);
            final Task task = TaskMockingUtils.mockTask(type, project, name);

            tasks.add(task);
            action.execute(task);
            return TaskMockingUtils.mockTaskProvider(task);
        });

        createValidDeobfuscatableJar(target);

        final DependencyDeobfuscator obfuscator = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertTrue(result.isPresent());

        final DependencyReplacementResult replacementResult = result.get();
        assertEquals(project, replacementResult.getProject());

        assertNotNull(replacementResult.getTaskNameBuilder());
        assertEquals("test", replacementResult.getTaskNameBuilder().apply("test"));

        assertNotNull(replacementResult.getSourcesJarTaskProvider());
        assertTrue(tasks.contains(replacementResult.getSourcesJarTaskProvider().get()));

        assertNotNull(replacementResult.getRawJarTaskProvider());
        assertTrue(tasks.contains(replacementResult.getRawJarTaskProvider().get()));

        assertNotNull(replacementResult.getAdditionalDependenciesConfiguration());
        assertTrue(replacementResult.getAdditionalDependenciesConfiguration().getDependencies().isEmpty());

        assertNotNull(replacementResult.getDependencyMetadataConfigurator());
        final DummyRepositoryEntry entry = new DummyRepositoryEntry(project);
        replacementResult.getDependencyMetadataConfigurator().accept(entry);
        assertEquals("fg.deobf.naming_channel.group", entry.getGroup());
        assertEquals("name", entry.getName());
        assertEquals("version", entry.getVersion());
        assertEquals("classifier", entry.getClassifier());
        assertEquals("extension", entry.getExtension());
        assertTrue(entry.getDependencies().isEmpty());

        assertNotNull(replacementResult.getAdditionalReplacements());
        assertTrue(replacementResult.getAdditionalReplacements().isEmpty());

        assertNotNull(replacementResult.getAdditionalIdePostSyncTasks());
        assertTrue(replacementResult.getAdditionalIdePostSyncTasks().isEmpty());
    }

    @Test
    public void weDeobfuscateADependencyWithASimpleJarWithoutDependenciesUsingACustomDeobfuscationGroupFromTheNamingChannel() throws IOException {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        final PathFile target = FileTestingUtils.newSimpleTestFile("some.jar");
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final List<Task> tasks = new ArrayList<>();
        final Mappings mappings = mock(Mappings.class);
        final Property<NamingChannel> namingChannelProperty = mock(Property.class);
        final NamingChannel namingChannel = mock(NamingChannel.class);
        final Property<String> deobfuscationGroupSupplier = mock(Property.class);

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(extensionContainer.getByType(Mappings.class)).thenReturn(mappings);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any()))
                .thenReturn(detachedConfiguration)
                .then((Answer<Configuration>) invocation -> {
                    final Dependency[] dependenciesIn = invocation.getArguments().length > 0 ? invocation.getArgument(0) : new Dependency[0];
                    final Configuration configuration = mock(Configuration.class);
                    final DependencySet dependencies = mock(DependencySet.class);
                    when(configuration.getDependencies()).thenReturn(dependencies);
                    when(configuration.getFiles()).thenThrow(new RuntimeException("Tried to resolve a detached configuration that was mocked for a situation where that should not happen."));
                    when(dependencies.isEmpty()).thenReturn(dependenciesIn.length == 0);
                    return configuration;
                });
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));
        when(resolvedArtifact.getFile()).thenReturn(target);
        when(resolvedDependency.getChildren()).thenReturn(Collections.emptySet());
        when(resolvedDependency.getName()).thenReturn("Dummy");
        when(resolvedDependency.getModuleGroup()).thenReturn("group");
        when(resolvedDependency.getModuleName()).thenReturn("name");
        when(resolvedDependency.getModuleVersion()).thenReturn("version");
        when(resolvedArtifact.getClassifier()).thenReturn("classifier");
        when(resolvedArtifact.getExtension()).thenReturn("extension");
        when(mappings.getChannel()).thenReturn(namingChannelProperty);
        when(namingChannelProperty.get()).thenReturn(namingChannel);
        when(namingChannel.getDeobfuscationGroupSupplier()).thenReturn(deobfuscationGroupSupplier);
        when(namingChannel.getName()).thenReturn("naming_channel");
        when(deobfuscationGroupSupplier.get()).thenReturn("some_group");

        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        when(taskContainer.register(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Class.class), ArgumentMatchers.any(Action.class))).thenAnswer(invocation -> {
            final String name = invocation.getArgument(0);
            final Class<? extends Task> type = invocation.getArgument(1);
            final Action action = invocation.getArgument(2);
            final Task task = TaskMockingUtils.mockTask(type, project, name);
            tasks.add(task);
            action.execute(task);
            return TaskMockingUtils.mockTaskProvider(task);
        });

        createValidDeobfuscatableJar(target);

        final DependencyDeobfuscator deobfuscator = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency dependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(dependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertTrue(result.isPresent());

        final DependencyReplacementResult replacementResult = result.get();
        assertEquals(project, replacementResult.getProject());

        assertNotNull(replacementResult.getTaskNameBuilder());
        assertEquals("test", replacementResult.getTaskNameBuilder().apply("test"));

        assertNotNull(replacementResult.getSourcesJarTaskProvider());
        assertTrue(tasks.contains(replacementResult.getSourcesJarTaskProvider().get()));

        assertNotNull(replacementResult.getRawJarTaskProvider());
        assertTrue(tasks.contains(replacementResult.getRawJarTaskProvider().get()));

        assertNotNull(replacementResult.getAdditionalDependenciesConfiguration());
        assertTrue(replacementResult.getAdditionalDependenciesConfiguration().getDependencies().isEmpty());

        assertNotNull(replacementResult.getDependencyMetadataConfigurator());
        final DummyRepositoryEntry entry = new DummyRepositoryEntry(project);
        replacementResult.getDependencyMetadataConfigurator().accept(entry);
        assertEquals("fg.deobf.some_group.group", entry.getGroup());
        assertEquals("name", entry.getName());
        assertEquals("version", entry.getVersion());
        assertEquals("classifier", entry.getClassifier());
        assertEquals("extension", entry.getExtension());
        assertTrue(entry.getDependencies().isEmpty());

        assertNotNull(replacementResult.getAdditionalReplacements());
        assertTrue(replacementResult.getAdditionalReplacements().isEmpty());

        assertNotNull(replacementResult.getAdditionalIdePostSyncTasks());
        assertTrue(replacementResult.getAdditionalIdePostSyncTasks().isEmpty());
    }

    @Test
    public void weDeobfuscateADependencyWithASimpleJarWithNormalDependencies() throws IOException {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        final PathFile target = FileTestingUtils.newSimpleTestFile("some.jar");
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final List<Task> tasks = new ArrayList<>();
        final Mappings mappings = mock(Mappings.class);
        final Property<NamingChannel> namingChannelProperty = mock(Property.class);
        final NamingChannel namingChannel = mock(NamingChannel.class);
        final Property<String> deobfuscationGroupSupplier = mock(Property.class);
        final PathFile dependentTarget = FileTestingUtils.newSimpleTestFile("dependent.jar");
        final ResolvedDependency dependency = mock(ResolvedDependency.class);
        final ResolvedArtifact dependencyArtifact = mock(ResolvedArtifact.class);

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(extensionContainer.getByType(Mappings.class)).thenReturn(mappings);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any()))
                .thenReturn(detachedConfiguration)
                .then((Answer<Configuration>) invocation -> {
                    final Dependency[] dependenciesIn = invocation.getArguments().length > 0 ? invocation.getArgument(0) : new Dependency[0];
                    final Configuration configuration = mock(Configuration.class);
                    final DependencySet dependencies = mock(DependencySet.class);
                    when(configuration.getDependencies()).thenReturn(dependencies);
                    when(configuration.getFiles()).thenThrow(new RuntimeException("Tried to resolve a detached configuration that was mocked for a situation where that should not happen."));
                    when(dependencies.isEmpty()).thenReturn(dependenciesIn.length == 0);
                    return configuration;
                });
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));
        when(resolvedArtifact.getFile()).thenReturn(target);
        when(resolvedDependency.getChildren()).thenReturn(Collections.singleton(dependency));
        when(resolvedDependency.getName()).thenReturn("Dummy");
        when(resolvedDependency.getModuleGroup()).thenReturn("group");
        when(resolvedDependency.getModuleName()).thenReturn("name");
        when(resolvedDependency.getModuleVersion()).thenReturn("version");
        when(resolvedArtifact.getClassifier()).thenReturn("classifier");
        when(resolvedArtifact.getExtension()).thenReturn("extension");
        when(mappings.getChannel()).thenReturn(namingChannelProperty);
        when(namingChannelProperty.get()).thenReturn(namingChannel);
        when(namingChannel.getDeobfuscationGroupSupplier()).thenReturn(deobfuscationGroupSupplier);
        when(namingChannel.getName()).thenReturn("naming_channel");
        when(deobfuscationGroupSupplier.get()).thenReturn("");

        when(dependency.getModuleArtifacts()).thenReturn(Collections.singleton(dependencyArtifact));
        when(dependencyArtifact.getFile()).thenReturn(dependentTarget);
        when(dependency.getChildren()).thenReturn(Collections.emptySet());
        when(dependency.getName()).thenReturn("Dependency");
        when(dependency.getModuleGroup()).thenReturn("dependent_group");
        when(dependency.getModuleName()).thenReturn("dependency");
        when(dependency.getModuleVersion()).thenReturn("some_classy_version");
        when(dependencyArtifact.getClassifier()).thenReturn("with_a_classifier");
        when(dependencyArtifact.getExtension()).thenReturn("and_an_extension");


        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        when(taskContainer.register(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Class.class), ArgumentMatchers.any(Action.class))).thenAnswer(invocation -> {
            final String name = invocation.getArgument(0);
            final Class<? extends Task> type = invocation.getArgument(1);
            final Action action = invocation.getArgument(2);
            final Task task = TaskMockingUtils.mockTask(type, project, name);
            tasks.add(task);
            action.execute(task);
            return TaskMockingUtils.mockTaskProvider(task);
        });

        createSimpleJar(dependentTarget);
        createValidDeobfuscatableJar(target);

        final DependencyDeobfuscator deobfuscator = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency externalModuleDependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(externalModuleDependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertTrue(result.isPresent());

        final DependencyReplacementResult replacementResult = result.get();
        assertEquals(project, replacementResult.getProject());

        assertNotNull(replacementResult.getTaskNameBuilder());
        assertEquals("test", replacementResult.getTaskNameBuilder().apply("test"));

        assertNotNull(replacementResult.getSourcesJarTaskProvider());
        assertTrue(tasks.contains(replacementResult.getSourcesJarTaskProvider().get()));

        assertNotNull(replacementResult.getRawJarTaskProvider());
        assertTrue(tasks.contains(replacementResult.getRawJarTaskProvider().get()));

        assertNotNull(replacementResult.getAdditionalDependenciesConfiguration());
        assertTrue(replacementResult.getAdditionalDependenciesConfiguration().getDependencies().isEmpty());

        assertNotNull(replacementResult.getDependencyMetadataConfigurator());
        final DummyRepositoryEntry entry = new DummyRepositoryEntry(project);
        replacementResult.getDependencyMetadataConfigurator().accept(entry);
        assertEquals("fg.deobf.naming_channel.group", entry.getGroup());
        assertEquals("name", entry.getName());
        assertEquals("version", entry.getVersion());
        assertEquals("classifier", entry.getClassifier());
        assertEquals("extension", entry.getExtension());
        assertFalse(entry.getDependencies().isEmpty());
        assertEquals(1, entry.getDependencies().size());

        final RepositoryReference dependencyEntry = entry.getDependencies().iterator().next();
        assertEquals("dependent_group", dependencyEntry.getGroup());
        assertEquals("dependency", dependencyEntry.getName());
        assertEquals("some_classy_version", dependencyEntry.getVersion());
        assertEquals("with_a_classifier", dependencyEntry.getClassifier());
        assertEquals("and_an_extension", dependencyEntry.getExtension());

        assertNotNull(replacementResult.getAdditionalReplacements());
        assertTrue(replacementResult.getAdditionalReplacements().isEmpty());

        assertNotNull(replacementResult.getAdditionalIdePostSyncTasks());
        assertTrue(replacementResult.getAdditionalIdePostSyncTasks().isEmpty());
    }


    @Test
    public void weDeobfuscateADependencyWithASimpleJarWithObfuscatedDependencies() throws IOException {
        AtomicReference<DependencyReplacer> replacer = new AtomicReference<>();

        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final DependencyReplacement dependencyReplacement = mock(DependencyReplacement.class);
        final ForcedDependencyDeobfuscationExtension forcedDependencyDeobfuscationExtension = mock(ForcedDependencyDeobfuscationExtension.class);
        final NamedDomainObjectContainer<DependencyReplacementHandler> handlers = mock();
        final ConfigurationContainer configurations = mock();
        final Configuration detachedConfiguration = mock();
        final ResolvedConfiguration resolvedConfiguration = mock();
        final LenientConfiguration lenientConfiguration = mock();
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        final PathFile target = FileTestingUtils.newSimpleTestFile("some.jar");
        final TaskContainer taskContainer = mock(TaskContainer.class);
        final List<Task> tasks = new ArrayList<>();
        final Mappings mappings = mock(Mappings.class);
        final Property<NamingChannel> namingChannelProperty = mock(Property.class);
        final NamingChannel namingChannel = mock(NamingChannel.class);
        final Property<String> deobfuscationGroupSupplier = mock(Property.class);
        final PathFile dependentTarget = FileTestingUtils.newSimpleTestFile("dependent.jar");
        final ResolvedDependency dependency = mock(ResolvedDependency.class);
        final ResolvedArtifact dependencyArtifact = mock(ResolvedArtifact.class);

        when(forcedDependencyDeobfuscationExtension.shouldDeobfuscate(any())).thenReturn(false);
        when(project.getExtensions()).thenReturn(extensionContainer);
        when(project.getTasks()).thenReturn(taskContainer);
        when(extensionContainer.getByType(ForcedDependencyDeobfuscationExtension.class)).thenReturn(forcedDependencyDeobfuscationExtension);
        when(extensionContainer.getByType(DependencyReplacement.class)).thenReturn(dependencyReplacement);
        when(extensionContainer.getByType(Mappings.class)).thenReturn(mappings);
        when(dependencyReplacement.getReplacementHandlers()).thenReturn(handlers);
        when(project.getConfigurations()).thenReturn(configurations);
        when(configurations.detachedConfiguration(any()))
                .thenReturn(detachedConfiguration)
                .then((Answer<Configuration>) invocation -> {
                    final Dependency[] dependenciesIn = invocation.getArguments().length > 0 ? invocation.getArgument(0) : new Dependency[0];
                    final Configuration configuration = mock(Configuration.class);
                    final DependencySet dependencies = mock(DependencySet.class);
                    when(configuration.getDependencies()).thenReturn(dependencies);
                    when(configuration.getFiles()).thenThrow(new RuntimeException("Tried to resolve a detached configuration that was mocked for a situation where that should not happen."));
                    when(dependencies.isEmpty()).thenReturn(dependenciesIn.length == 0);
                    return configuration;
                });
        when(detachedConfiguration.getResolvedConfiguration()).thenReturn(resolvedConfiguration);
        when(resolvedConfiguration.getLenientConfiguration()).thenReturn(lenientConfiguration);
        when(lenientConfiguration.getFiles()).thenReturn(Collections.singleton(mock(File.class)));
        when(lenientConfiguration.getFirstLevelModuleDependencies()).thenReturn(Collections.singleton(resolvedDependency));
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));
        when(resolvedArtifact.getFile()).thenReturn(target);
        when(resolvedDependency.getChildren()).thenReturn(Collections.singleton(dependency));
        when(resolvedDependency.getName()).thenReturn("Dummy");
        when(resolvedDependency.getModuleGroup()).thenReturn("group");
        when(resolvedDependency.getModuleName()).thenReturn("name");
        when(resolvedDependency.getModuleVersion()).thenReturn("version");
        when(resolvedArtifact.getClassifier()).thenReturn("classifier");
        when(resolvedArtifact.getExtension()).thenReturn("extension");
        when(mappings.getChannel()).thenReturn(namingChannelProperty);
        when(namingChannelProperty.get()).thenReturn(namingChannel);
        when(namingChannel.getDeobfuscationGroupSupplier()).thenReturn(deobfuscationGroupSupplier);
        when(namingChannel.getName()).thenReturn("naming_channel");
        when(deobfuscationGroupSupplier.get()).thenReturn("");

        when(dependency.getModuleArtifacts()).thenReturn(Collections.singleton(dependencyArtifact));
        when(dependencyArtifact.getFile()).thenReturn(dependentTarget);
        when(dependency.getChildren()).thenReturn(Collections.emptySet());
        when(dependency.getName()).thenReturn("Dependency");
        when(dependency.getModuleGroup()).thenReturn("dependent_group");
        when(dependency.getModuleName()).thenReturn("dependency");
        when(dependency.getModuleVersion()).thenReturn("some_classy_version");
        when(dependencyArtifact.getClassifier()).thenReturn("with_a_classifier");
        when(dependencyArtifact.getExtension()).thenReturn("and_an_extension");


        when(handlers.create(ArgumentMatchers.eq("obfuscatedDependencies"), any(Action.class)))
                .thenAnswer(invocation -> {
                    final Action<DependencyReplacementHandler> action = invocation.getArgument(1);
                    final DependencyReplacementHandler handler = mock(DependencyReplacementHandler.class);
                    final Property<DependencyReplacer> property = mock(Property.class);

                    doAnswer(invocation1 -> {
                        replacer.set(invocation1.getArgument(0));
                        return null;
                    }).when(property).set(ArgumentMatchers.<DependencyReplacer>any());
                    when(handler.getReplacer()).thenReturn(property);

                    action.execute(handler);
                    return handler;
                });

        when(taskContainer.register(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Class.class), ArgumentMatchers.any(Action.class))).thenAnswer(invocation -> {
            final String name = invocation.getArgument(0);
            final Class<? extends Task> type = invocation.getArgument(1);
            final Action action = invocation.getArgument(2);
            final Task task = TaskMockingUtils.mockTask(type, project, name);
            tasks.add(task);
            action.execute(task);
            return TaskMockingUtils.mockTaskProvider(task);
        });

        createValidDeobfuscatableJar(dependentTarget);
        createValidDeobfuscatableJar(target);

        final DependencyDeobfuscator obfuscator = new DependencyDeobfuscator(project) {};

        final DependencyReplacer sut = replacer.get();

        //First validate that the was properly registered and retrieved
        assertNotNull(sut);

        final Context context = mock(Context.class);
        final ExternalModuleDependency externalModuleDependency = mock(ExternalModuleDependency.class);
        when(context.getDependency()).thenReturn(externalModuleDependency);
        when(context.getProject()).thenReturn(project);

        final Optional<DependencyReplacementResult> result = sut.get(context);
        assertNotNull(result);
        assertTrue(result.isPresent());

        final DependencyReplacementResult replacementResult = result.get();
        assertEquals(project, replacementResult.getProject());

        assertNotNull(replacementResult.getTaskNameBuilder());
        assertEquals("test", replacementResult.getTaskNameBuilder().apply("test"));

        assertNotNull(replacementResult.getSourcesJarTaskProvider());
        assertTrue(tasks.contains(replacementResult.getSourcesJarTaskProvider().get()));

        assertNotNull(replacementResult.getRawJarTaskProvider());
        assertTrue(tasks.contains(replacementResult.getRawJarTaskProvider().get()));

        assertNotNull(replacementResult.getAdditionalDependenciesConfiguration());
        assertTrue(replacementResult.getAdditionalDependenciesConfiguration().getDependencies().isEmpty());

        assertNotNull(replacementResult.getDependencyMetadataConfigurator());
        final DummyRepositoryEntry entry = new DummyRepositoryEntry(project);
        replacementResult.getDependencyMetadataConfigurator().accept(entry);
        assertEquals("fg.deobf.naming_channel.group", entry.getGroup());
        assertEquals("name", entry.getName());
        assertEquals("version", entry.getVersion());
        assertEquals("classifier", entry.getClassifier());
        assertEquals("extension", entry.getExtension());
        assertFalse(entry.getDependencies().isEmpty());
        assertEquals(1, entry.getDependencies().size());

        final RepositoryReference dependencyEntry = entry.getDependencies().iterator().next();
        assertEquals("fg.deobf.naming_channel.dependent_group", dependencyEntry.getGroup());
        assertEquals("dependency", dependencyEntry.getName());
        assertEquals("some_classy_version", dependencyEntry.getVersion());
        assertEquals("with_a_classifier", dependencyEntry.getClassifier());
        assertEquals("and_an_extension", dependencyEntry.getExtension());

        assertNotNull(replacementResult.getAdditionalReplacements());
        assertFalse(replacementResult.getAdditionalReplacements().isEmpty());
        assertEquals(1, replacementResult.getAdditionalReplacements().size());
        DependencyReplacementResult additionalReplacement = replacementResult.getAdditionalReplacements().iterator().next();
        assertEquals(project, additionalReplacement.getProject());

        assertNotNull(additionalReplacement.getTaskNameBuilder());
        assertEquals("test", additionalReplacement.getTaskNameBuilder().apply("test"));

        assertNotNull(additionalReplacement.getSourcesJarTaskProvider());
        assertTrue(tasks.contains(additionalReplacement.getSourcesJarTaskProvider().get()));

        assertNotNull(additionalReplacement.getRawJarTaskProvider());
        assertTrue(tasks.contains(additionalReplacement.getRawJarTaskProvider().get()));

        assertNotNull(additionalReplacement.getAdditionalDependenciesConfiguration());
        assertTrue(additionalReplacement.getAdditionalDependenciesConfiguration().getDependencies().isEmpty());

        assertNotNull(additionalReplacement.getDependencyMetadataConfigurator());
        final DummyRepositoryEntry dependencyRawEntry = new DummyRepositoryEntry(project);
        additionalReplacement.getDependencyMetadataConfigurator().accept(dependencyRawEntry);
        assertEquals("fg.deobf.naming_channel.dependent_group", dependencyRawEntry.getGroup());
        assertEquals("dependency", dependencyRawEntry.getName());
        assertEquals("some_classy_version", dependencyRawEntry.getVersion());
        assertEquals("with_a_classifier", dependencyRawEntry.getClassifier());
        assertEquals("and_an_extension", dependencyRawEntry.getExtension());
        assertTrue(dependencyRawEntry.getDependencies().isEmpty());

        assertNotNull(additionalReplacement.getAdditionalIdePostSyncTasks());
        assertTrue(additionalReplacement.getAdditionalIdePostSyncTasks().isEmpty());

        assertNotNull(replacementResult.getAdditionalIdePostSyncTasks());
        assertTrue(replacementResult.getAdditionalIdePostSyncTasks().isEmpty());
    }

    private static void createSimpleJar(PathFile target) throws IOException {
        final ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target.toPath()));
        final ZipEntry manifest = new ZipEntry("META-INF/MANIFEST.MF");
        zip.putNextEntry(manifest);
        zip.write("Manifest-Version: 1.0".getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.flush();
        zip.close();
    }

    private static void createValidDeobfuscatableJar(PathFile target) throws IOException {
        final ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target.toPath()));
        final ZipEntry manifest = new ZipEntry("META-INF/MANIFEST.MF");
        zip.putNextEntry(manifest);
        zip.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
        zip.write("Obfuscated: true\n".getBytes(StandardCharsets.UTF_8));
        zip.write("Obfuscated-By: ForgeGradle\n".getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.flush();
        zip.close();
    }

}