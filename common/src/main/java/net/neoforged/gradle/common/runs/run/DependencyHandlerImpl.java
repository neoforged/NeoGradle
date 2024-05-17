package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.runs.run.DependencyHandler;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DependencyHandlerImpl implements DependencyHandler {

    private final Project project;
    private final String context;

    @Inject
    public DependencyHandlerImpl(Project project, String context) {
        this.project = project;
        this.context = context;
    }

    public Project getProject() {
        return project;
    }

    public Configuration getRuntimeConfiguration() {
        final Configuration configuration = ConfigurationUtils.temporaryConfiguration(project, context);
        if (configuration.getDependencies().isEmpty()) {
            configuration.fromDependencyCollector(this.getRuntime());
        }
        return configuration;
    }
}
