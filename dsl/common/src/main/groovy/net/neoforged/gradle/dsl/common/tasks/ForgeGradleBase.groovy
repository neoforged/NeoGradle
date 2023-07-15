package net.neoforged.gradle.dsl.common.tasks

import groovy.transform.CompileStatic;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.LogLevel;

/**
 * Base class for all ForgeGradle tasks.
 */
@CompileStatic
abstract class ForgeGradleBase extends DefaultTask implements WithWorkspace {

    ForgeGradleBase() {
        setGroup("ForgeGradle");

        getLogging().captureStandardOutput(LogLevel.DEBUG);
        getLogging().captureStandardError(LogLevel.ERROR);
    }
}
