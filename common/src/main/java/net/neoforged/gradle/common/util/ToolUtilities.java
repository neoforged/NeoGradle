package net.neoforged.gradle.common.util;

import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.util.ModuleDependencyUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.repositories.ArtifactRepository;

import java.io.File;
import java.util.function.Supplier;

public class ToolUtilities {

    private ToolUtilities() {
        throw new IllegalStateException("Tried to create utility class!");
    }

    public static File resolveTool(final Project project, final String tool) {
        return resolveTool(project, () -> ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                "ToolLookupFor" + ModuleDependencyUtils.toConfigurationName(tool),
                project.getDependencies().create(tool)
        ).getFiles().iterator().next());
    }

    public static ResolvedArtifact resolveToolArtifact(final Project project, final String tool) {
        return resolveTool(project, () ->  ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                "ToolLookupFor" + ModuleDependencyUtils.toConfigurationName(tool),
                project.getDependencies().create(tool)
        ).getResolvedConfiguration().getResolvedArtifacts().iterator().next());
    }

    public static ResolvedArtifact resolveToolArtifact(final Project project, final Dependency tool) {
        return resolveTool(project, () -> ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                "ToolLookupFor" + ModuleDependencyUtils.toConfigurationName(tool),
                tool
        ).getResolvedConfiguration().getResolvedArtifacts().iterator().next());
    }

    private static <T> T resolveTool(final Project project, final Supplier<T> searcher) {
        //Grab the dynamic repository
        final Repository repository = project.getExtensions().getByType(Repository.class);
        final ArtifactRepository artifactRepository = repository.getRepository();

        //Remove it from the project, tools can never be in there, and we can get into trouble else-
        project.getRepositories().remove(artifactRepository);

        //Resolve the tool
        final T resolvedArtifact = searcher.get();

        //Re-add the repository
        project.getRepositories().add(artifactRepository);

        //Return the resolved artifact
        return resolvedArtifact;
    }
}
