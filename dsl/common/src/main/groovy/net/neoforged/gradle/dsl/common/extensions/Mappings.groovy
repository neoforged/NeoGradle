package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.minecraftforge.gdi.annotations.ProjectGetter
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Defines the an object which handles the configuration of the mappings system.
 */
@CompileStatic
interface Mappings extends BaseDSLElement<Mappings> {

    /**
     * @return The project this extension belongs to.
     */
    @ProjectGetter
    Project getProject();

    /**
     * The mcp minecraft extension this mappings extension belongs to.
     *
     * @return The mcp minecraft extension this mappings extension belongs to.
     */
    Minecraft getMinecraft();

    /**
     * The channel to pull the mappings from.
     *
     * @return The channel to pull the mappings from.
     */
    @Input
    @Optional
    @DSLProperty
    Property<NamingChannel> getChannel();

    /**
     * The version to pull the mappings from.
     *
     * @return The version to pull the mappings from.
     */
    @Input
    @Optional
    @DSLProperty
    MapProperty<String, String> getVersion();
}
