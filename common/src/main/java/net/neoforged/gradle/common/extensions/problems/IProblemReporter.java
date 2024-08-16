package net.neoforged.gradle.common.extensions.problems;

import org.gradle.api.Action;
import org.gradle.api.logging.Logger;

/**
 * NeoGradle problems reporter API
 */
public interface IProblemReporter {

    /**
     * Reports a problem to the user without stopping the build.
     *
     * @param spec An action that configures the problem spec
     * @param logger A logger to log the problem to
     */
    void reporting(Action<NeoGradleProblemSpec> spec, Logger logger);

    /**
     * Reports a problem to the user and stops the build.
     *
     * @param spec An action that configures the problem spec
     * @return A runtime exception to throw
     */
    RuntimeException throwing(Action<NeoGradleProblemSpec> spec);
}
