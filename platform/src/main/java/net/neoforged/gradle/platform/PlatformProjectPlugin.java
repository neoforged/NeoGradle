package net.neoforged.gradle.platform;

import net.neoforged.gradle.common.CommonPlugin;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.dsl.common.runs.ide.extensions.IdeaRunExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.userdev.extension.UserDev;
import net.neoforged.gradle.neoform.NeoFormPlugin;
import net.neoforged.gradle.platform.extensions.DynamicProjectExtension;
import net.neoforged.gradle.platform.runtime.runtime.definition.RuntimeDevRuntimeDefinition;
import net.neoforged.gradle.userdev.extension.UserDevExtension;
import net.neoforged.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlatformProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project target) {
        target.getPlugins().apply(CommonPlugin.class);
        target.getExtensions().create("dynamicProject", DynamicProjectExtension.class, target);
        
        target.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.configureEach(run -> configureRun(target, run)));
    }
    
    private void configureRun(final Project project, final Run run) {
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName("main");
        
        run.getConfigureAutomatically().set(false);
        
        run.getModSources().add(mainSourceSet);
        
        project.getExtensions().getByType(IdeManagementExtension.class).onIdea((project1, idea, ideaExtension) -> run.getExtensions().getByType(IdeaRunExtension.class).getPrimarySourceSet().convention(mainSourceSet));
    }
}
