package net.neoforged.gradle.dsl.common.extensions.subsystems.conventions

import net.neoforged.gdi.BaseDSLElement
import net.neoforged.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property

/**
 * Allows configuration of the individual conventions related to jarJar used by NeoGradle.
 */
interface JarJar extends BaseDSLElement<JarJar> {

    /**
     * Global flag to enable or disable the jarJar conventions system. If disabled, no conventions jarJar features will be created or used.
     * Note: this can not be configured via the buildscript and needs to be set in the gradle.properties file.
     */
    @DSLProperty
    Property<Boolean> getIsEnabled()

    /**
     * Whether or not the default jarJar feature for the main source set should be created.
     * When set to {@code false}, NeoGradle will not register the {@code jarJar} task or its configuration,
     * leaving consumers free to define their own jarJar features via {@link net.neoforged.gradle.dsl.common.extensions.JarJar#forFeature(String)}.
     */
    @DSLProperty
    Property<Boolean> getShouldDefaultMainFeatureBeCreated()
}