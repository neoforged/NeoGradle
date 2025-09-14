package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.runs.DevLogin;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunDevLoginOptions;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class RunDevLoginOptionsImpl implements RunDevLoginOptions {

    private final Project project;

    @Inject
    public RunDevLoginOptionsImpl(Project project, Run run) {
        this.project = project;

        final DevLogin devLogin = project.getExtensions().getByType(Subsystems.class).getConventions().getRuns().getDevLogin();
        getIsEnabled().convention(devLogin.getConventionForRun().zip(run.getIsClient(), (devLoginRun, runIsClient) -> devLoginRun && runIsClient));
    }

    @Override
    public Project getProject() {
        return project;
    }
}
