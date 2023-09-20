package net.neoforged.gradle.dsl.common.tasks

import groovy.transform.CompileStatic;
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ProviderFactory

import javax.inject.Inject
import javax.naming.spi.ObjectFactory;

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
