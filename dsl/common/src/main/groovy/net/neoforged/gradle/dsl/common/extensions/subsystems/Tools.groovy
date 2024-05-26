package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Allows configuration of Parchment mappings for userdev.
 */
@CompileStatic
interface Tools extends ConfigurableDSLElement<Parchment> {


    /**
     * Artifact coordinates for JST.
     * Used by the parchment subsystem to generate mappings, and by the AT subsystem to apply access transformers to source.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getJST();

}