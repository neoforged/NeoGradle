package net.minecraftforge.gradle.dsl.common.extensions;

import net.minecraftforge.gradle.dsl.annotations.DSLProperty
import net.minecraftforge.gradle.dsl.base.ProjectAssociatedBaseDSLElement
import net.minecraftforge.gradle.dsl.base.util.ConfigurableDSLElement;
import net.minecraftforge.gradle.dsl.base.util.ProjectAssociatedDSLElement
import org.gradle.api.provider.Property;

/**
 * Defines a project extension object which manages deobfuscation of artifacts.
 */
interface Deobfuscation extends ProjectAssociatedBaseDSLElement<Deobfuscation> {

    /**
     * @returns The version of the forge flower artifact that should be used.
     */
    @DSLProperty(propertyName = "forgeFlowerVersion")
    Property<String> getForgeFlowerVersion();
}
