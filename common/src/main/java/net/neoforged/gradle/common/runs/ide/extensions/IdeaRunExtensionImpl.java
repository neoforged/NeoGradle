package net.neoforged.gradle.common.runs.ide.extensions;

import net.neoforged.gradle.dsl.common.runs.ide.extensions.IdeaRunExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;

import javax.inject.Inject;

public abstract class IdeaRunExtensionImpl implements IdeaRunExtension {

    private final Project project;
    private final Run run;

    @Inject
    public IdeaRunExtensionImpl(Project project, Run run) {
        this.project = project;
        this.run = run;

        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        this.getPrimarySourceSet().convention(javaPluginExtension.getSourceSets().getAt("main"));
    }

    @Override
    public Project getProject() {
        return project;
    }
}
