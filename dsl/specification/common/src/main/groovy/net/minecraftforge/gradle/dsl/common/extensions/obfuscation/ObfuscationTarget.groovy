package net.minecraftforge.gradle.dsl.common.extensions.obfuscation

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.DSLProperty
import net.minecraftforge.gradle.dsl.base.BaseDSLElement
import net.minecraftforge.gradle.dsl.base.util.DistributionType
import net.minecraftforge.gradle.dsl.base.util.NamedDSLElement
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Defines a target for obfuscation.
 */
@CompileStatic
interface ObfuscationTarget extends BaseDSLElement<ObfuscationTarget>, NamedDSLElement {

    /**
     * Allows for optionally setting the minecraft version which is in use for this obfuscation target.
     *
     * This needs to be configured if the minecraft version can not be resolved via dependency resolution.
     * @return The minecraft version which is in use for this obfuscation target.
     */
    @Optional
    @Input
    @DSLProperty
    Property<String> getMinecraftVersion();

    /**
     * Allows for optionally setting the distribution type which is in use for this obfuscation target.
     *
     * This needs to be configured if the minecraft version can not be resolved via dependency resolution.
     * @return The distribution type which is in use for this obfuscation target.
     */
    @Optional
    @Input
    @DSLProperty
    Property<DistributionType> getDistributionType();
}
