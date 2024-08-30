package net.neoforged.gradle.dsl.platform.dynamic

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property

/**
 * Represents the configuration for a NeoForm dynamic project.
 */
@CompileStatic
abstract class NeoFormProjectConfiguration implements ConfigurableDSLElement<NeoFormProjectConfiguration> {

    /**
     * @return True if the source sets should be split based on distribution, false otherwise.
     */
    @DSLProperty
    abstract Property<Boolean> getSplitSourceSets();
}
