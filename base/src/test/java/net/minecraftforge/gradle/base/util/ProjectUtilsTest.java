package net.minecraftforge.gradle.base.util;

import net.minecraftforge.gradle.base.extensions.ProjectEvaluationExtension;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectUtilsTest {

    @Test
    void afterEvaluatePassesTheHandlingToTheDedicatedExtension() {
        final Project project = mock(Project.class);
        final ExtensionContainer extensionContainer = mock(ExtensionContainer.class);
        final ProjectEvaluationExtension projectEvaluationExtension = mock(ProjectEvaluationExtension.class);

        when(project.getExtensions()).thenReturn(extensionContainer);
        when(extensionContainer.getByType(ProjectEvaluationExtension.class)).thenReturn(projectEvaluationExtension);

        final Runnable target = () -> { };

        ProjectUtils.afterEvaluate(project, target);

        verify(projectEvaluationExtension).afterEvaluate(target);
    }
}