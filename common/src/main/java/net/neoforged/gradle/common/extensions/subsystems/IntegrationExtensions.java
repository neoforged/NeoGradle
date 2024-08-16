package net.neoforged.gradle.common.extensions.subsystems;

import net.neoforged.gradle.common.extensions.base.WithEnabledProperty;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Integration;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class IntegrationExtensions extends WithEnabledProperty implements Integration {

    @Inject
    public IntegrationExtensions(Project project) {
        super(project, "integrations");

        getUseGradleProblemReporting().set(
                getBooleanProperty("gradle-problem-reporting", false, false)
        );
    }
}
