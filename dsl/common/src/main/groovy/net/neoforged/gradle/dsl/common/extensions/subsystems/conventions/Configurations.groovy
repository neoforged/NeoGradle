package net.neoforged.gradle.dsl.common.extensions.subsystems.conventions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property

/**
 * Allows configuration of the individual conventions related to configurations used by NeoGradle.
 */
@CompileStatic
interface Configurations extends BaseDSLElement<Configurations> {

    /**
     * Global flag to enable or disable the configurations conventions system. If disabled, no conventions configurations will be created or used.
     * Note: this can not be configured via the buildscript and needs to be set in the gradle.properties file.
     */
    @DSLProperty
    Property<Boolean> getIsEnabled();

    /**
     * The post-fix to apply to the local runtime configuration.
     * Note: this can be configured via the buildscript, however any sourcesets created earlier then the setting statement will not have the post-fix correctly applied, it is as such recommended to use the gradle.properties file.
     */
    @DSLProperty
    Property<String> getLocalRuntimeConfigurationPostFix();

    /**
     * The post-fix to apply to the per-source-set runtime configuration added to a runs compileDependencies
     * Note: this can be configured via the buildscript, however any sourcesets created earlier then the setting statement will not have the post-fix correctly applied, it is as such recommended to use the gradle.properties file.
     */
    @DSLProperty
    Property<String> getRunRuntimeConfigurationPostFix();

    /**
     * The post-fix to apply to the per-run configuration added to a runs compileDependencies
     */
    @DSLProperty
    Property<String> getPerRunRuntimeConfigurationPostFix();

    /**
     * The identifier of the runtime configuration that will be used for the local runtime configuration.
     * Note: this can not be configured via the buildscript and needs to be set in the gradle.properties file.
     */
    @DSLProperty
    Property<String> getRunRuntimeConfigurationName();
}