package net.neoforged.gradle.common.extensions;


import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.problems.ProblemReporter;
import org.gradle.api.problems.ProblemSpec;
import org.gradle.api.problems.Severity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class NeoGradleProblemReporter {

    private final Project project;
    private final ProblemReporter delegate;

    public NeoGradleProblemReporter(Project project, ProblemReporter delegate) {
        this.project = project;
        this.delegate = delegate;
    }

    public void reporting(Action<ProblemSpec> spec) {
        delegate.reporting(spec);
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

    private String readMeUrl(String section) {
        final String neogradleVersion;
        try(final InputStream stream = Objects.requireNonNull(getClass().getClassLoader().getResource("version.neogradle")).openStream()) {
            neogradleVersion = new String(stream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read NeoGradle version", e);
        }

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
    }
}
