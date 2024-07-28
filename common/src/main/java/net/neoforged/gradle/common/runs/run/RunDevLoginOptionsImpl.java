package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.extensions.subsystems.DevLogin;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunDevLoginOptions;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class RunDevLoginOptionsImpl implements RunDevLoginOptions {

    @Inject
    public RunDevLoginOptionsImpl(Project project, Run run) {

        final DevLogin devLogin = project.getExtensions().getByType(Subsystems.class).getDevLogin();
        getIsEnabled().convention(devLogin.getConventionForRun().zip(run.getIsClient(), (conventionForRun, isClient) -> conventionForRun && isClient));
    }
}
