package net.neoforged.gradle.common.extensions.problems;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.logging.Logger;
import org.gradle.api.problems.Severity;
import org.gradle.api.problems.ProblemReporter;

import javax.inject.Inject;

/**
 * Implements a problem reporter that integrates with the Gradles own problem reporter, while
 * also logging directly to the user where needed
 */
public class IntegratedProblemReporter implements IProblemReporter {

    private final ProblemReporter delegate;

    @Inject
    public IntegratedProblemReporter(ProblemReporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void reporting(Action<NeoGradleProblemSpec> spec, Logger logger) {
        delegate.reporting(problemSpec -> {
            final NeoGradleProblemSpec neoGradleProblemSpec = new NeoGradleProblemSpec();
            spec.execute(neoGradleProblemSpec);

            problemSpec.id(neoGradleProblemSpec.category(), neoGradleProblemSpec.id())
                    .contextualLabel(neoGradleProblemSpec.contextualLabel())
                    .solution(neoGradleProblemSpec.solution())
                    .details(neoGradleProblemSpec.details())
                    .severity(Severity.WARNING)
                    .documentedAt(neoGradleProblemSpec.documentedAt());

            neoGradleProblemSpec.log(logger);
        });
    }

    @Override
    public RuntimeException throwing(Action<NeoGradleProblemSpec> spec) {
        return delegate.throwing(problemSpec -> {
            final NeoGradleProblemSpec neoGradleProblemSpec = new NeoGradleProblemSpec();
            spec.execute(neoGradleProblemSpec);

            problemSpec.id(neoGradleProblemSpec.category(), neoGradleProblemSpec.id())
                    .contextualLabel(neoGradleProblemSpec.contextualLabel())
                    .solution(neoGradleProblemSpec.solution())
                    .details(neoGradleProblemSpec.details())
                    .severity(Severity.ERROR)
                    .withException(new InvalidUserDataException( "(%s) %s.\nPotential Solution: %s.\nMore information: %s".formatted(
                            neoGradleProblemSpec.contextualLabel(),
                            neoGradleProblemSpec.details(),
                            neoGradleProblemSpec.solution(),
                            neoGradleProblemSpec.documentedAt()
                    )))
                    .documentedAt(neoGradleProblemSpec.documentedAt());
        });
    }
}
