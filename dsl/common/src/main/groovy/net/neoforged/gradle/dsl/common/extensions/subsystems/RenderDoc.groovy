package net.neoforged.gradle.dsl.common.extensions.subsystems

import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface RenderDoc extends ConfigurableDSLElement<RenderDoc> {

    /**
     * @return The suffix for the configuration name to use when adding the render doc configuration.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getConfigurationSuffix()
}