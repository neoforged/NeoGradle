package net.neoforged.gradle.dsl.mixin.extension

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.ConfigurableFileCollection

@CompileStatic
interface Mixin extends BaseDSLElement<Mixin> {
    String EXTENSION_NAME = "mixin";

    @DSLProperty
    ConfigurableFileCollection getConfigs();

    /**
     * Adds mixin configuration files to the mixin configuration. <br>
     * Supports every type supported by {@link org.gradle.api.Project#files(Object...)}.
     * @param configs The mixin configuration files
     */
    void config(Object... configs);
}