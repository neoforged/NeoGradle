package net.neoforged.gradle.platform;

import net.neoforged.gradle.common.CommonPlugin;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.dsl.common.runs.ide.extensions.IdeaRunExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import net.neoforged.gradle.platform.extensions.DynamicProjectExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

public class PlatformProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project target) {
        target.getPlugins().apply(CommonPlugin.class);
        target.getExtensions().create("dynamicProject", DynamicProjectExtension.class, target);
        
        target.getExtensions().configure(RunManager.class, runs -> runs.configureAll(run -> configureRun(target, run)));
    }
    
    private void configureRun(final Project project, final Run run) {
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName("main");
        
        run.getConfigureAutomatically().set(false);
        
        run.getModSources().add(mainSourceSet);
        
        project.getExtensions().getByType(IdeManagementExtension.class).onIdea((project1, rootProject, idea, ideaExtension) -> run.getExtensions().getByType(IdeaRunExtension.class).getPrimarySourceSet().convention(mainSourceSet));
    }
}
