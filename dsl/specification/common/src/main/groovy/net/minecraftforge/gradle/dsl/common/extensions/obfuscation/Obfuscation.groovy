package net.minecraftforge.gradle.dsl.common.extensions.obfuscation

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.DSLProperty
import net.minecraftforge.gradle.dsl.base.BaseDSLElement
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property
import org.jetbrains.annotations.NotNull

/**
 * Defines a project component which manages the configuration of the obfuscation logic.
 */
@CompileStatic
interface Obfuscation extends BaseDSLElement<Obfuscation> {


    /**
     * Indicates if the system should automatically obfuscate all Jar tasks.
     *
     * @return The indicator if the system should obfuscate all jar tasks automatically.
     */
    @NotNull
    @DSLProperty
    Property<Boolean> getCreateAutomatically();

    /**
     * The manual configurations of the obfuscation targets.
     * The name of the target needs to match the name of the obfuscation target task.
     *
     * @return The manual configurations of the obfuscation targets.
     */
    @NotNull
    @DSLProperty
    NamedDomainObjectContainer<ObfuscationTarget> getTargets();
}
