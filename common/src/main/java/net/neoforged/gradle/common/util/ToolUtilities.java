package net.neoforged.gradle.common.util;

import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;

import java.io.File;

public class ToolUtilities {

    private ToolUtilities() {
        throw new IllegalStateException("Tried to create utility class!");
    }

    public static File resolveTool(final Project project, final String tool) {
        return ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                project.getDependencies().create(tool)
        ).getFiles().iterator().next();
    }

    public static ResolvedArtifact resolveToolArtifact(final Project project, final String tool) {
        return ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                project.getDependencies().create(tool)
        ).getResolvedConfiguration().getResolvedArtifacts().iterator().next();
    }

    public static ResolvedArtifact resolveToolArtifact(final Project project, final Dependency tool) {
        return ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                tool
        ).getResolvedConfiguration().getResolvedArtifacts().iterator().next();
    }
}
