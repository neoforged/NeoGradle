package net.minecraftforge.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property

/**
 * Defines a project extension object which manages deobfuscation of artifacts.
 */
@CompileStatic
interface Deobfuscation extends BaseDSLElement<Deobfuscation> {

    /**
     * The version of forge flower which should be used to perform decompilation if an obfuscated artifact does not provide sources.
     *
     * @return The version of the forge flower artifact that should be used.
     */
    @DSLProperty
    Property<String> getForgeFlowerVersion();
}
