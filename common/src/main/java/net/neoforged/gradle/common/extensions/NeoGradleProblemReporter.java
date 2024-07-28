package net.neoforged.gradle.common.extensions;


import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.problems.ProblemReporter;
import org.gradle.api.problems.ProblemSpec;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class NeoGradleProblemReporter implements ProblemReporter {

    private final Project project;
    private final ProblemReporter delegate;

    public NeoGradleProblemReporter(Project project, ProblemReporter delegate) {
        this.project = project;
        this.delegate = delegate;
    }

    @Override
    public void reporting(Action<ProblemSpec> spec) {
        delegate.reporting(spec);
    }

    @Override
    public RuntimeException throwing(Action<ProblemSpec> spec) {
        return delegate.throwing(spec);
    }

    @Override
    public RuntimeException rethrowing(RuntimeException e, Action<ProblemSpec> spec) {
        return delegate.rethrowing(e, spec);
    }

    public String readMeUrl(String section) {
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
}
