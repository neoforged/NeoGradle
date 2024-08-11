package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

/**
 * Allows configuration of Parchment mappings for userdev.
 */
@CompileStatic
interface Parchment extends ConfigurableDSLElement<Parchment> {

    /**
     * Artifact coordinates for parchment mappings.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getParchmentArtifact();

    /**
     * The prefix added to parameters in parchment when a conflict arises.
     */
    @Input
    @DSLProperty
    Property<String> getConflictPrefix();

    /**
     * Minecraft version of parchment to use. This property is
     * ignored if {@link #getParchmentArtifact()} is set explicitly.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getMinecraftVersion();

    /**
     * Mapping version of default parchment to use. This property is
     * ignored if {@link #getParchmentArtifact()} is set explicitly.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getMappingsVersion();

    /**
     * If enabled (the default), the parchment repository will automatically be added to the project,
     * if {@link #getIsEnabled()} is true.
     */
    @Internal
    @DSLProperty
    Property<Boolean> getAddRepository();

    /**
     * Enables or disables the system. It is enabled by default if a {@link #getParchmentArtifact()} is specified.
     */
    @Input
    @DSLProperty
    Property<Boolean> getIsEnabled();

}
