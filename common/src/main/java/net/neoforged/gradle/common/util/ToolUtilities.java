package net.neoforged.gradle.common.util;

import net.neoforged.gradle.util.ModuleDependencyUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;

import java.io.File;
import java.util.function.Supplier;

public class ToolUtilities {

    private ToolUtilities() {
        throw new IllegalStateException("Tried to create utility class!");
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
