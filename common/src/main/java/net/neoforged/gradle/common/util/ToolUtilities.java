package net.neoforged.gradle.common.util;

import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Tools;
import net.neoforged.gradle.util.ModuleDependencyUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.Dependencies;
import org.gradle.api.artifacts.dsl.DependencyCollector;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ToolUtilities {

    private ToolUtilities() {
        throw new IllegalStateException("Tried to create utility class!");
    }

    public static Provider<File> resolveTool(final Project project, final Function<Tools, Provider<String>> tool) {
        return resolveTool(
                project,
                tool.apply(
                        project.getExtensions().getByType(Subsystems.class).getTools()
                )
        );
    }

    public static Provider<File> resolveTool(final Project project, final Provider<String> tool) {
        //We use an anonymous dependency collector here so that we can convert the tool coordinate
        //to a file outside the scope of the provider itself.
        //If we were to use the provider directly, for example via map, the lambda would need to capture
        //the project that converts the string to a dependency.
        //This breaks the configuration cache as Projects can not be serialized.
        final DependencyCollector collector = project.getObjects().dependencyCollector();
        collector.add(tool.map(project.getDependencies()::create));
        final Configuration config = ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                "Tool"
        );
        config.fromDependencyCollector(collector);
        return config.getIncoming().getArtifacts().getResolvedArtifacts().map(a -> a.iterator().next())
                .map(ResolvedArtifactResult::getFile);
    }

    public static File resolveTool(final Project project, final String tool) {
        return resolveTool(() -> ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                "ToolLookupFor" + ModuleDependencyUtils.toConfigurationName(tool),
                project.getDependencies().create(tool)
        ).getFiles().iterator().next());
    }

    public static ResolvedArtifact resolveToolArtifact(final Project project, final String tool) {
        return resolveTool(() ->  ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                "ToolLookupFor" + ModuleDependencyUtils.toConfigurationName(tool),
                project.getDependencies().create(tool)
        ).getResolvedConfiguration().getResolvedArtifacts().iterator().next());
    }

    public static ResolvedArtifact resolveToolArtifact(final Project project, final Dependency tool) {
        return resolveTool(() -> ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                "ToolLookupFor" + ModuleDependencyUtils.toConfigurationName(tool),
                tool
        ).getResolvedConfiguration().getResolvedArtifacts().iterator().next());
    }

    private static <T> T resolveTool(final Supplier<T> searcher) {
        //Return the resolved artifact
        return searcher.get();
    }
}
