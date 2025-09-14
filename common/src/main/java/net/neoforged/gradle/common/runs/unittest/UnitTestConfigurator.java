package net.neoforged.gradle.common.runs.unittest;

import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.runs.run.Running;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;

public class UnitTestConfigurator {

    public static void configureIdeUnitTests(final Project project) {
        IdeManagementExtension ide = project.getExtensions().getByType(IdeManagementExtension.class);

        ide.onIdea((ideaProject, rootProject, idea, ideaExtension) -> {
            final ExtensionAware extensionAware = (ExtensionAware) idea;
            if (extensionAware.getExtensions().findByType(Running.class) != null) {
                return;
            }

            //The actual project that this run is associated with does not matter,
            //So we can just use the idea project, we can use the root project,
            //because it is not guaranteed to have all the needed extensions.
            final Running ideaDefaultTestRun = RunsUtil.create(ideaProject, "idea");
            extensionAware.getExtensions().add(Running.class, "unitTests", ideaDefaultTestRun);

            ideaDefaultTestRun.getIsJUnit().set(true);
            ideaDefaultTestRun.runType("junit");
        });
    }
}
