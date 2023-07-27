package net.neoforged.gradle.dsl.common.tasks

import groovy.transform.CompileStatic;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.LogLevel;

/**
 * Base class for all NeoGradle tasks.
 */
@CompileStatic
abstract class NeoGradleBase extends DefaultTask implements WithWorkspace {

    NeoGradleBase() {
        setGroup("NeoGradle");

        getLogging().captureStandardOutput(LogLevel.DEBUG);
        getLogging().captureStandardError(LogLevel.ERROR);
    }
}
