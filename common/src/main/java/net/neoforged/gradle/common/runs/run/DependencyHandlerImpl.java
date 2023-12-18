package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.runs.run.DependencyHandler;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

import javax.inject.Inject;
import java.util.Map;

public abstract class DependencyHandlerImpl implements DependencyHandler {

    private final Project project;

    private final Configuration configuration;

    @Inject
    public DependencyHandlerImpl(Project project) {
        this.project = project;
        this.configuration = project.getConfigurations().detachedConfiguration();
        this.configuration.setCanBeResolved(true);
        this.configuration.setCanBeConsumed(false);
        this.configuration.setTransitive(false);
    }

    public Project getProject() {
        return project;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Dependency runtime(Object dependencyNotation) {
        if (dependencyNotation instanceof Configuration) {
            this.configuration.extendsFrom((Configuration) dependencyNotation);
            return null;
        }
        final Dependency dependency = project.getDependencies().create(dependencyNotation);
        configuration.getDependencies().add(dependency);
        return dependency;
    }

    @Override
    public Dependency runtime(Object dependencyNotation, Action<Dependency> configureClosure) {
        if (dependencyNotation instanceof Configuration) {
            if (configureClosure != null) {
                throw new GradleException("Cannot add a Configuration with a configuration closure.");
            }
            this.configuration.extendsFrom((Configuration) dependencyNotation);
            return null;
        }
        final Dependency dependency = project.getDependencies().create(dependencyNotation);
        configureClosure.execute(dependency);
        configuration.getDependencies().add(dependency);
        return dependency;
    }

    @Override
    public Dependency create(Object dependencyNotation) {
        final Dependency dependency = project.getDependencies().create(dependencyNotation);
        configuration.getDependencies().add(dependency);
        return dependency;
    }

    @Override
    public Dependency create(Object dependencyNotation, Action<Dependency> configureClosure) {
        final Dependency dependency = project.getDependencies().create(dependencyNotation);
        configureClosure.execute(dependency);
        return dependency;
    }

    @Override
    public Dependency module(Object notation) {
        return project.getDependencies().module(notation);
    }

    @Override
    public Dependency module(Object notation, Action<Dependency> configureClosure) {
        final Dependency dependency = project.getDependencies().module(notation);
        configureClosure.execute(dependency);
        return dependency;
    }

    @Override
    public Dependency project(Map<String, ?> notation) {
        final Dependency dependency = project.getDependencies().project(notation);
        return dependency;
    }
}
