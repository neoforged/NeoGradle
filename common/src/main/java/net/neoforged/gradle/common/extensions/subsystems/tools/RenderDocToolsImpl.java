package net.neoforged.gradle.common.extensions.subsystems.tools;

import net.neoforged.gradle.dsl.common.extensions.subsystems.tools.RenderDocTools;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class RenderDocToolsImpl implements RenderDocTools {

    private final Project project;

    @Inject
    public RenderDocToolsImpl(Project project) {
        this.project = project;

        getRenderDocPath().convention(getProject().getLayout().getBuildDirectory().dir("renderdoc"));
        getRenderDocVersion().convention("1.33");
        getRenderNurse().convention("net.neoforged:render-nurse:0.0.12");
    }

    @Override
    public Project getProject() {
        return project;
    }
}
