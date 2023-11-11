package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Allows configuration of the decompiler used by NeoGradle.
 */
@CompileStatic
interface Decompiler extends ConfigurableDSLElement<Decompiler> {

    /**
     * Allows the maximum memory provided to the decompiler to be overridden. Must be specified
     * in the "123g" or "123m" form.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getMaxMemory();

    /**
     * Allows the maximum number of threads used by the decompiler to be constrained. By default, it will
     * use all available threads.
     */
    @Input
    @Optional
    @DSLProperty
    Property<Integer> getMaxThreads();

    /**
     * The log-level to use for the decompiler. Supported values: info, debug, warn, error.
     * Defaults to info.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getLogLevel();

    /**
     * Allows additional JVM arguments to be added to the decompiler invocation.
     */
    @Input
    @Optional
    @DSLProperty
    ListProperty<String> getJvmArgs();

}
