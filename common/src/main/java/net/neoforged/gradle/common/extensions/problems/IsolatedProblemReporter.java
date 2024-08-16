package net.neoforged.gradle.common.extensions.problems;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.logging.Logger;

public class IsolatedProblemReporter implements IProblemReporter {

    @Override
    public void reporting(Action<NeoGradleProblemSpec> spec, Logger logger) {
        final NeoGradleProblemSpec neoGradleProblemSpec = new NeoGradleProblemSpec();
        spec.execute(neoGradleProblemSpec);
        neoGradleProblemSpec.log(logger);
    }

    @Override
    public RuntimeException throwing(Action<NeoGradleProblemSpec> spec) {
        final NeoGradleProblemSpec neoGradleProblemSpec = new NeoGradleProblemSpec();
        spec.execute(neoGradleProblemSpec);

        throw new InvalidUserDataException( "(%s) %s.\nPotential Solution: %s.\nMore information: %s".formatted(
                neoGradleProblemSpec.contextualLabel(),
                neoGradleProblemSpec.details(),
                neoGradleProblemSpec.solution(),
                neoGradleProblemSpec.documentedAt()
        ));
    }
}
