package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.neoforged.gdi.BaseDSLElement
import net.neoforged.gdi.annotations.DSLProperty
import org.gradle.api.file.DirectoryProperty

/**
 * Defines the extension which holds the configuration data.
 */
@CompileStatic
interface ConfigurationData extends BaseDSLElement<ConfigurationData> {

    /**
     * The location of the configuration data.
     */
    @DSLProperty
    DirectoryProperty getLocation();
}