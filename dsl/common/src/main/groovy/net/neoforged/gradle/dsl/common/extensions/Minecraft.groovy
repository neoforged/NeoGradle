package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import org.jetbrains.annotations.NotNull

@CompileStatic
interface Minecraft extends BaseDSLElement<Minecraft> {

    /**
     * The mod identifier of the current project.
     * <p>
     * Defaults to a sanitized version of the project's {@linkplain org.gradle.api.Project#getName name}.
     *
     * @return The mod identifier of the current project.
     */
    @DSLProperty
    Property<String> getModIdentifier();

    /**
     * Gives access to all naming channels that are known in the current project.
     *
     * @return The naming channels that are known in the current project.
     */
    @DSLProperty
    NamedDomainObjectContainer<NamingChannel> getNamingChannels();

    /**
     * Gives access to the mappings configuration extension.
     * Allows for the configuration of the mappings system, including which naming channel to use, and what version information to use.
     *
     * @return The mappings configuration extension.
     */
    @NotNull
    @DSLProperty
    Mappings getMappings();

    /**
     * Gives access to the access transformer configuration extension.
     *
     * @return The access transformer configuration extension.
     */
    @NotNull
    @DSLProperty
    AccessTransformers getAccessTransformers();
}
