package net.neoforged.gradle.common.runs.unittest;

import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.runs.ide.IdeRunIntegrationManager;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;

public class UnitTestConfigurator {

    public static void configureIdeUnitTests(final Project project) {
        IdeManagementExtension ide = project.getExtensions().getByType(IdeManagementExtension.class);

        ide.onIdea((ideaProject, idea, ideaExtension) -> {
            final ExtensionAware extensionAware = (ExtensionAware) idea;

            final Run ideaDefaultTestRun = RunsUtil.create(ideaProject, "ideaDefaultUnitTesting");
            extensionAware.getExtensions().add(Run.class, "unitTests", ideaDefaultTestRun);

            ideaDefaultTestRun.getIsJUnit().set(true);
            ideaDefaultTestRun.configure("junit");

            RunManager runManager = project.getExtensions().getByType(RunManager.class);
            runManager.addInternal(ideaDefaultTestRun);
        });
    }
}
