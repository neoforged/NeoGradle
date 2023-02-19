package net.minecraftforge.gradle.dsl.runs.type

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.ClosureEquivalent
import net.minecraftforge.gradle.dsl.base.util.NamedDSLObjectContainer
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider

/**
 * Defines a DSL extension which allows for the registration of run types.
 */
@CompileStatic
interface Types extends NamedDSLObjectContainer<Types, Type> {

    /**
     * Registers a new run type with the given name.
     * Potentially adding a prefix if the name is already in use.
     *
     * @param prefix The prefix to add to the name if it is already in use.
     * @param name The name of the run type.
     * @param configurationAction The action to configure the run type with.
     * @return The created or existing run type.
     */
    @ClosureEquivalent
    NamedDomainObjectProvider<Type> registerWithPotentialPrefix(String prefix, String name, Action<? super Type> configurationAction)
}