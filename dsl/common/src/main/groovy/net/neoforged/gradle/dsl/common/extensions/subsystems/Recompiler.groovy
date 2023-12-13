package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Allows configuration of the java compiler NeoGradle uses to compile the decompiled and patched Minecraft
 * source code.
 */
@CompileStatic
interface Recompiler extends ConfigurableDSLElement<Recompiler> {

    /**
     * Allows the maximum memory provided to the decompiler to be overridden. Must be specified
     * in the "123g" or "123m" form.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getMaxMemory();

    /**
     * Allows additional JVM arguments to be added to the JVM that is forked for running the compiler.
     */
    @Input
    @Optional
    @DSLProperty
    ListProperty<String> getJvmArgs();

    /**
     * Allows additional arguments to be added to the compiler.
     */
    @Input
    @Optional
    @DSLProperty
    ListProperty<String> getArgs();

}
