package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.DirectoryProperty
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
    Property<Boolean> getIsEnabled()

    /**
     * @returns The path to where RenderDoc is installed, or will be installed if the directory is empty or none-existent
     * @implNote This will check for the existence of a library file in the correct location for the underlying OS installation.
     */
    @DSLProperty
    @Input
    @Optional
    DirectoryProperty getRenderDocPath()

    /**
     * @returns The version of RenderDoc to use
     */
    @DSLProperty
    @Input
    Property<String> getVersion()

}