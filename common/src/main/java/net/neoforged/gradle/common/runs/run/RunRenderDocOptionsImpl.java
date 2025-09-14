package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.runs.RenderDoc;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunRenderDocOptions;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class RunRenderDocOptionsImpl implements RunRenderDocOptions {

    private final Project project;

    @Inject
    public RunRenderDocOptionsImpl(Project project, Run run) {
        this.project = project;

        final RenderDoc renderDoc = project.getExtensions().getByType(Subsystems.class).getConventions().getRuns().getRenderDoc();
        getEnabled().convention(renderDoc.getConventionForRun().zip(run.getIsClient(), (renderDocRun, runIsClient) -> renderDocRun && runIsClient));
    }

    @Override
    public Project getProject() {
        return project;
    }
}
