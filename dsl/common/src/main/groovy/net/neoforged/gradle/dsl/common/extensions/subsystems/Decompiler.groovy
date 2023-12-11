package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import groovy.transform.Internal
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
    @Internal
    @Optional
    @DSLProperty
    Property<String> getMaxMemory();

    /**
     * Allows the maximum number of threads used by the decompiler to be constrained. By default, it will
     * use all available threads.
     */
    @Internal
    @Optional
    @DSLProperty
    Property<Integer> getMaxThreads();

    /**
     * The log-level to use for the decompiler. Supported values: trace, info, warn, error.
     * Defaults to {@link DecompilerLogLevel#INFO}.
     */
    @Internal
    @Optional
    @DSLProperty
    Property<DecompilerLogLevel> getLogLevel();

    /**
     * Allows additional JVM arguments to be added to the decompiler invocation.
     */
    @Internal
    @Optional
    @DSLProperty
    ListProperty<String> getJvmArgs();

}
