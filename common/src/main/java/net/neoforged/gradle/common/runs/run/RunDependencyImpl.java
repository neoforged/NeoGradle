package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.runs.run.RunDependency;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class RunDependencyImpl implements RunDependency {

    private final Project project;
    
    @Inject
    public RunDependencyImpl(Project project, Dependency dependency) {
        getIdentity().convention(dependency.toString());
        getDependency().from(project.provider(() -> {
            final Configuration configuration = ConfigurationUtils.temporaryConfiguration(project, dependency);
            final ResolvedConfiguration resolvedConfiguration = configuration.getResolvedConfiguration();
            final ConfigurableFileCollection files = project.files();
            return files.from(resolvedConfiguration.getFiles());
        }));
        this.project = project;
    }
    
    @Override
    public Project getProject() {
        return project;
    }
    
    @Override
    public abstract ConfigurableFileCollection getDependency();

    @Override
    public abstract Property<String> getIdentity();
}
