package net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.ide

import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Allows configuration of the individual conventions related to IDEA IDE used by NeoGradle.
 */
interface IDEA extends BaseDSLElement<IDEA> {

    /**
     * Global flag to enable or disable the IDEA conventions system. If disabled, no conventions IDEA will be created or used.
     */
    @DSLProperty
    Property<Boolean> getIsEnabled();

    /**
     * Whether or not the IDEA conventions should use compiler detection.
     */
    @DSLProperty
    Property<Boolean> getShouldUseCompilerDetection();

    /**
     * Whether or not the IDEA sync should use the post sync task.
     */
    @DSLProperty
    Property<Boolean> getShouldUsePostSyncTask();
}
