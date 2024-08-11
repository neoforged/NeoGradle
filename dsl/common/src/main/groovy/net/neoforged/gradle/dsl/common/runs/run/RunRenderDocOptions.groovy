package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Options for running a run with RenderDoc
 */
@CompileStatic
interface RunRenderDocOptions extends BaseDSLElement<RunRenderDocOptions> {

    /**
     * @returns Whether RenderDoc is enabled
     */
    @DSLProperty
    @Input
    @Optional
    Property<Boolean> getEnabled()
}