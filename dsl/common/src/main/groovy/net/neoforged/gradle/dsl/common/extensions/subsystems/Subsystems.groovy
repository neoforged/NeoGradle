package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.tasks.Nested

/**
 * Allows configuration of various NeoGradle subsystems.
 */
@CompileStatic
interface Subsystems extends BaseDSLElement<Subsystems> {

    /**
     * @return settings for the decompiler subsystem
     */
    @Nested
    @DSLProperty
    Decompiler getDecompiler();

    /**
     * @return settings for the recompiler subsystem
     */
    @Nested
    @DSLProperty
    Recompiler getRecompiler();

    /**
     * @return settings for the neoform caching subsystem
     */
    @Nested
    @DSLProperty
    NeoFormCache getNeoFormCache();

}
