package net.neoforged.gradle.dsl.common.runtime.naming;

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement;
import net.minecraftforge.gdi.annotations.DSLProperty;
import org.gradle.api.provider.Property
import org.jetbrains.annotations.NotNull;

/**
 * Defines a channel for a naming scheme.
 * Handles the creation of tasks for applying and un-applying mappings to a jar being it a sources or compiled jar.
 */
@CompileStatic
interface NamingChannel extends BaseDSLElement<NamingChannel> {

    /**
     * The name of the naming channel.
     *
     * @return The name.
     */
    @NotNull
    String getName();

    /**
     * The builder which can construct a new task provider for a source jar mapping.
     * Every time this is invoked the builder needs to produce a new task.
     * However, the builder is allowed to reuse an old task if the inputs match.
     *
     * @return The builder property.
     */
    @DSLProperty
    Property<ApplyMappingsToSourceJarTaskBuilder> getApplySourceMappingsTaskBuilder();

    /**
     * The builder which can construct a new task provider for a compiled jar mapping.
     * Every time this is invoked the builder needs to produce a new task.
     * However, the builder is allowed to reuse an old task if the inputs match.
     *
     * @return The builder property.
     */
    @DSLProperty
    Property<ApplyMappingsToCompiledJarTaskBuilder> getApplyCompiledMappingsTaskBuilder();

    /**
     * @return Indicates if the user has accepted this mappings license.
     */
    @DSLProperty
    Property<Boolean> getHasAcceptedLicense();

    /**
     * @return The license text of this mappings channel.
     */
    @DSLProperty
    Property<String> getLicenseText();
}
