package net.neoforged.gradle.common.extensions.subsystems;

import net.neoforged.gradle.common.extensions.subsystems.tools.RenderDocToolsImpl;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Tools;
import net.neoforged.gradle.dsl.common.extensions.subsystems.tools.RenderDocTools;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class ToolsExtension implements Tools {

    private final RenderDocTools renderDocTools;

    @Inject
    public ToolsExtension(Project project) {
        renderDocTools = project.getObjects().newInstance(RenderDocToolsImpl.class, project);
    }

    @Override
    public RenderDocTools getRenderDoc() {
        return renderDocTools;
    }
}
