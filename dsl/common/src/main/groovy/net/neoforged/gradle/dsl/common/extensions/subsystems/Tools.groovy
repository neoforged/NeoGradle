package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.extensions.subsystems.tools.RenderDocTools
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

/**
 * Allows configuration of Parchment mappings for userdev.
 */
@CompileStatic
interface Tools extends ConfigurableDSLElement<Tools> {


    /**
     * Artifact coordinates for JST.
     * Used by the parchment subsystem to generate mappings, and by the AT subsystem to apply access transformers to source.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getJST();

    /**
     * Artifact coordinates for the NeoGradle decompiler.
     * Used by the runs subsystem to allow login to the dev environment.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getDevLogin();

    /**
     * Tool configuration for RenderDoc and RenderNurse.
     * @return The RenderDoc tool configuration.
     */
    @Nested
    RenderDocTools getRenderDoc();

    /**
     * Artifact coordinates for the binary patcher.
     * Used by dynamic projects to create and use binary patches.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getBinaryPatcher();

    /**
     * Artifact coordinates for the binary access transformer tool.
     * Used by the vanilla subsystem to apply access transformers to binary classes.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getAccessTransformer();

    /**
     * Artifact coordinates for the AutoRenamingTool.
     * Used by different subsystems to rename classes in jars.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getAutoRenamingTool();

    /**
     * Artifact coordinates for the NeoGradle decompiler.
     * Used by the vanilla subsystem to decompile the game.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getDecompiler();

    /**
     * Artifact coordinates for the installer tools.
     * Used by the platform to configure different tooling in production and userdev.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getInstallerTools();

    /**
     * Artifact coordinates for the jar splitter.
     * Used by the platform to split jars into smaller jars.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getJarSplitter();
}