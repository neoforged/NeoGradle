package net.neoforged.gradle.dsl.common.extensions.subsystems.conventions

import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property

/**
 * Allows configuration of the individual conventions related to runs used by NeoGradle.
 */
interface Runs extends BaseDSLElement<Runs> {

    /**
     * Global flag to enable or disable the runs conventions system. If disabled, no conventions runs will be created or used.
     * Note: this can not be configured via the buildscript and needs to be set in the gradle.properties file.
     */
    @DSLProperty
    Property<Boolean> getIsEnabled()

    /**
     * Whether or not the default runs should be created.
     */
    @DSLProperty
    Property<Boolean> getShouldDefaultRunsBeCreated()

    /**
     * Whether or not the default test task should be reused.
     */
    @DSLProperty
    Property<Boolean> getShouldDefaultTestTaskBeReused();
}