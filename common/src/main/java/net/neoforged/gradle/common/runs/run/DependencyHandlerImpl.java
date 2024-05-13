package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.runs.run.DependencyHandler;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import javax.inject.Inject;
import java.util.Map;

public abstract class DependencyHandlerImpl implements DependencyHandler {

    private final Project project;

    @Inject
    public DependencyHandlerImpl(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public Configuration getConfiguration() {
        final Configuration configuration = ConfigurationUtils.temporaryConfiguration(project);
        configuration.fromDependencyCollector(this.getRuntime());
        return configuration;
    }
}
