package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.extensions.problems.IProblemReporter;
import net.neoforged.gradle.common.interfaceinjection.InterfaceInjectionPublishing;
import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.dsl.common.extensions.InterfaceInjections;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public abstract class InterfaceInjectionsExtension implements InterfaceInjections {
    private transient final DependencyHandler projectDependencies;
    private transient final ArtifactHandler projectArtifacts;

    private final Project project;

    @SuppressWarnings("UnstableApiUsage")
    @Inject
    public InterfaceInjectionsExtension(final Project project) {
        this.project = project;

        this.projectDependencies = project.getDependencies();
        this.projectArtifacts = project.getArtifacts();
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void expose(Object path, Action<ConfigurablePublishArtifact> action) {
        getFiles().from(path);
        projectArtifacts.add(InterfaceInjectionPublishing.INTERFACE_INJECTION_ELEMENTS_CONFIGURATION, path, action);
    }

    @Override
    public void expose(Object path) {
        expose(path, artifacts -> {
        });
    }

    @SuppressWarnings("removal")
    @Override
    public void expose(Dependency dependency) {
        project.getExtensions().getByType(IProblemReporter.class)
                .reporting(
                    spec -> spec.id("interface-injections", "expose.deprecated")
                        .details("Using the expose(Dependency) method is deprecated.")
                        .solution("Use the dependency collectors: 'consume' and 'consumeApi' for adding interface injections from your dependencies.")
                        .section("userdev-interface-injections-from-dependencies")
                        .contextualLabel("Deprecations"),
                    project.getLogger()
                );

        projectDependencies.add(InterfaceInjectionPublishing.INTERFACE_INJECTION_API_CONFIGURATION, dependency);
    }
}
