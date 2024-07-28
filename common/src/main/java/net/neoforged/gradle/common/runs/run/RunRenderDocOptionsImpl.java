package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.runs.run.RunRenderDocOptions;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class RunRenderDocOptionsImpl implements RunRenderDocOptions {

    private final Project project;

    @Inject
    public RunRenderDocOptionsImpl(Project project) {
        this.project = project;

        getEnabled().convention(false);
        getRenderDocPath().convention(getProject().getLayout().getBuildDirectory().dir("renderdoc"));
        getRenderDocVersion().convention("1.33");
        getRenderNurseVersion().convention("0.0.9");
    }

    @Override
    public Project getProject() {
        return project;
    }
}
