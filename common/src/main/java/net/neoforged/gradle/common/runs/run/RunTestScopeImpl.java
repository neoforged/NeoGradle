package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.runs.run.RunTestScope;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class RunTestScopeImpl implements RunTestScope {

    private final Project project;

    @Inject
    public RunTestScopeImpl(Project project) {
        this.project = project;
    }

    @Override
    public Project getProject() {
        return project;
    }
}
