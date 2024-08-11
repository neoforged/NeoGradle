package net.neoforged.gradle.dsl.common.extensions.subsystems.tools

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

/**
 * Tool configuration for RenderDoc and RenderNurse.
 */
@CompileStatic
interface RenderDocTools extends BaseDSLElement<RenderDocTools> {

    /**
     * @return The artifact coordinate for RenderNurse.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getRenderNurse();

    /**
     * @return The artifact version for RenderDoc.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getRenderDocVersion();

    /**
     * @return The path to where RenderDoc is installed, or will be installed if the directory is empty or none-existent
     */
    @Internal
    @Optional
    @DSLProperty
    DirectoryProperty getRenderDocPath();
}