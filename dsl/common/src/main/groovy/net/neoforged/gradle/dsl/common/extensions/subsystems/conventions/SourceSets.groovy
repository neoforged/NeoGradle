package net.neoforged.gradle.dsl.common.extensions.subsystems.conventions

import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property

/**
 * Allows configuration of the source set conventions used by NeoGradle.
 */
interface SourceSets extends BaseDSLElement<SourceSets> {

    /**
     * Global flag to enable or disable the source set conventions system. If disabled, all source set conventions will be ignored.
     * Note: this can not be configured via the buildscript and needs to be set in the gradle.properties file.
     */
    @DSLProperty
    Property<Boolean> getIsEnabled();

    /**
     * Whether or not the main source set should be automatically added to runs.
     */
    @DSLProperty
    Property<Boolean> getShouldMainSourceSetBeAutomaticallyAddedToRuns();

    /**
     * Whether or not the test source set should be automatically added to runs.
     */
    @DSLProperty
    Property<Boolean> getShouldTestSourceSetBeAutomaticallyAddedToRuns();

    /**
     * Whether or not the local run runtime configuration should be automatically added to runs.
     * @see net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.Configurations#getRunRuntimeConfigurationPostFix()
     */
    @DSLProperty
    Property<Boolean> getShouldSourceSetsLocalRunRuntimesBeAutomaticallyAddedToRuns();


}