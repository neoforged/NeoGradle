package net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.runs

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Defines conventions for the render doc system.
 */
@CompileStatic
interface RenderDoc extends BaseDSLElement<RenderDoc> {

    /**
     * Global flag to enable or disable the runs render doc conventions system. If disabled, no conventions runs will be created or used.
     * Note: this can not be configured via the buildscript and needs to be set in the gradle.properties file.
     */
    @DSLProperty
    Property<Boolean> getIsEnabled()

    /**
     * @return The default usage flag state for runs. This is by default false (meaning the render doc configuration is not used by default), setting this to true will make all clients use render doc by default.
     */
    @Input
    @Optional
    @DSLProperty
    Property<Boolean> getConventionForRun()
}
