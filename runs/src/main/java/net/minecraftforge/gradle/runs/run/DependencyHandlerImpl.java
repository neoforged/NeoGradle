package net.minecraftforge.gradle.runs.run;

import net.minecraftforge.gradle.dsl.runs.run.DependencyHandler;
import net.minecraftforge.gradle.dsl.runs.run.RunDependency;
import org.gradle.api.Action;
import org.gradle.api.Project;
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

    @Override
    public RunDependency runtime(Object dependencyNotation) {
        final Dependency dependency = project.getDependencies().create(dependencyNotation);
        final RunDependency runDependency = project.getObjects().newInstance(RunDependencyImpl.class, project, dependency);
        getRuntime().add(runDependency);
        return runDependency;
    }

    @Override
    public RunDependency runtime(Object dependencyNotation, Action<Dependency> configureClosure) {
        final Dependency dependency = project.getDependencies().create(dependencyNotation);
        configureClosure.execute(dependency);
        final RunDependency runDependency = project.getObjects().newInstance(RunDependencyImpl.class, project, dependency);
        getRuntime().add(runDependency);
        return runDependency;
    }

    @Override
    public RunDependency create(Object dependencyNotation) {
        final Dependency dependency = project.getDependencies().create(dependencyNotation);
        return project.getObjects().newInstance(RunDependencyImpl.class, project, dependency);
    }

    @Override
    public RunDependency create(Object dependencyNotation, Action<Dependency> configureClosure) {
        final Dependency dependency = project.getDependencies().create(dependencyNotation);
        configureClosure.execute(dependency);
        return project.getObjects().newInstance(RunDependencyImpl.class, project, dependency);
    }

    @Override
    public RunDependency module(Object notation) {
        final Dependency dependency = project.getDependencies().module(notation);
        return project.getObjects().newInstance(RunDependencyImpl.class, project, dependency);
    }

    @Override
    public RunDependency module(Object notation, Action<Dependency> configureClosure) {
        final Dependency dependency = project.getDependencies().module(notation);
        configureClosure.execute(dependency);
        return project.getObjects().newInstance(RunDependencyImpl.class, project, dependency);
    }

    @Override
    public RunDependency project(Map<String, ?> notation) {
        final Dependency dependency = project.getDependencies().project(notation);
        return project.getObjects().newInstance(RunDependencyImpl.class, project, dependency);
    }
}
