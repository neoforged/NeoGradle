package net.neoforged.gradle.common.extensions.problems;

import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.gradle.api.Project;
import org.gradle.api.problems.Problems;

public class ProblemReportingConfigurator {

    public static final String PROBLEM_NAMESPACE = "neoforged.gradle";
    public static final String PROBLEM_REPORTER_EXTENSION_NAME = "neogradleProblems";

    public static void configureProblemReporting(Project project, Problems problems) {
        final boolean enableGradleProblemReporting = project.getExtensions().getByType(Subsystems.class).getIntegration().getUseGradleProblemReporting().get();
        if (enableGradleProblemReporting) {
            project.getExtensions().create(IProblemReporter.class, PROBLEM_REPORTER_EXTENSION_NAME, IntegratedProblemReporter.class, problems.forNamespace(PROBLEM_NAMESPACE));
        } else {
            project.getExtensions().create(IProblemReporter.class, PROBLEM_REPORTER_EXTENSION_NAME, IsolatedProblemReporter.class);
        }
    }
}
