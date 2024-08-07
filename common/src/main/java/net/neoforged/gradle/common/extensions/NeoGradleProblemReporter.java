package net.neoforged.gradle.common.extensions;


import net.neoforged.gradle.common.util.NeoGradleUtils;
import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.logging.Logger;
import org.gradle.api.problems.ProblemReporter;
import org.gradle.api.problems.ProblemSpec;
import org.gradle.api.problems.Severity;

public class NeoGradleProblemReporter {

    private final ProblemReporter delegate;

    public NeoGradleProblemReporter(ProblemReporter delegate) {
        this.delegate = delegate;
    }

    public void reporting(Action<NeoGradleProblemSpec> spec, Logger logger) {
        delegate.reporting(problemSpec -> {
            final NeoGradleProblemSpec neoGradleProblemSpec = new NeoGradleProblemSpec();
            spec.execute(neoGradleProblemSpec);

            final String url = readMeUrl(neoGradleProblemSpec.section);

            problemSpec.id(neoGradleProblemSpec.category, neoGradleProblemSpec.id)
                    .contextualLabel(neoGradleProblemSpec.contextualLabel)
                    .solution(neoGradleProblemSpec.solution)
                    .details(neoGradleProblemSpec.details)
                    .severity(Severity.WARNING)
                    .documentedAt(url);

            neoGradleProblemSpec.log(logger);
        });


    }

    public RuntimeException throwing(Action<NeoGradleProblemSpec> spec) {
        return delegate.throwing(problemSpec -> {
            final NeoGradleProblemSpec neoGradleProblemSpec = new NeoGradleProblemSpec();
            spec.execute(neoGradleProblemSpec);

            final String url = readMeUrl(neoGradleProblemSpec.section);

            problemSpec.id(neoGradleProblemSpec.category, neoGradleProblemSpec.id)
                    .contextualLabel(neoGradleProblemSpec.contextualLabel)
                    .solution(neoGradleProblemSpec.solution)
                    .details(neoGradleProblemSpec.details)
                    .severity(Severity.ERROR)
                    .withException(new InvalidUserDataException( "(%s) %s.\nPotential Solution: %s.\nMore information: %s".formatted(
                            neoGradleProblemSpec.contextualLabel,
                            neoGradleProblemSpec.details,
                            neoGradleProblemSpec.solution,
                            url
                    )))
                    .documentedAt(url);
        });
    }

    private static String readMeUrl(String section) {
        final String neogradleVersion = NeoGradleUtils.getNeogradleVersion();
        final String branchMajorVersion = neogradleVersion.split("\\.")[0];
        final String branchMinorVersion = neogradleVersion.split("\\.")[1];
        final String branch = "NG_%s.%s".formatted(branchMajorVersion, branchMinorVersion);

        return "https://github.com/neoforged/NeoGradle/blob/%s/README.md#%s".formatted(branch, section);
    }

    public static final class NeoGradleProblemSpec {
        private String category;
        private String id;
        private String contextualLabel;
        private String solution;
        private String details;
        private String section;

        public NeoGradleProblemSpec id(String category, String id) {
            this.category = category;
            this.id = id;
            return this;
        }

        public NeoGradleProblemSpec contextualLabel(String contextualLabel) {
            this.contextualLabel = contextualLabel;
            return this;
        }

        public NeoGradleProblemSpec solution(String solution) {
            this.solution = solution;
            return this;
        }

        public NeoGradleProblemSpec details(String details) {
            this.details = details;
            return this;
        }

        public NeoGradleProblemSpec section(String section) {
            this.section = section;
            return this;
        }

        private void log(Logger logger) {
            logger.warn("-------------------------------------");
            logger.warn("NeoGradle detected a problem with your project: %s".formatted(contextualLabel));
            logger.warn("Details: %s".formatted(details));
            logger.warn("Potential Solution: %s".formatted(solution));
            logger.warn("More information: %s".formatted(readMeUrl(section)));
            logger.warn("-------------------------------------");
        }
    }
}
