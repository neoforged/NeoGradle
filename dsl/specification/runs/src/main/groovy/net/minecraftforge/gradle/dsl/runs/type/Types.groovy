package net.minecraftforge.gradle.dsl.runs.type

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider

/**
 * Defines a DSL extension which allows for the registration of run types.
 */
@CompileStatic
interface Types extends NamedDomainObjectContainer<Type> {

    /**
     * Registers a new run type with the given name.
     *
     * @param prefix The prefix to use if the name is already in use.
     * @param name The name of the run type.
     * @param configurationAction The configuration action to apply to the run type.
     * @return The run type.
     */
    NamedDomainObjectProvider<Type> registerWithPotentialPrefix(String prefix, String name, Action<? super Type> configurationAction);
}