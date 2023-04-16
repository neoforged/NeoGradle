package net.minecraftforge.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.minecraftforge.gradle.dsl.common.runtime.naming.NamingChannel
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.annotations.NotNull

@CompileStatic
interface Minecraft extends BaseDSLElement<Minecraft> {

    /**
     * Gives access to all naming channels that are known in the current project.
     *
     * @return The naming channels that are known in the current project.
     */
    @DSLProperty
    NamedDomainObjectContainer<NamingChannel> getNamingChannelProviders();

    /**
     * Gives access to the mappings configuration extension.
     * Allows for the configuration of the mappings system, including which naming channel to use, and what version information to use.
     *
     * @return The mappings configuration extension.
     */
    @NotNull
    Mappings getMappings();

    /**
     * Gives access to the access transformer configuration extension.
     *
     * @return The access transformer configuration extension.
     */
    @NotNull
    AccessTransformers getAccessTransformers();

    /**
     * Gives access to the deobfuscation configuration extension.
     *
     * @return The deobfuscation configuration extension.
     */
    @NotNull
    Deobfuscation getDeobfuscation();
}
