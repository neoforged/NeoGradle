package net.minecraftforge.gradle.dsl.base.util

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.base.BaseDSLElement
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.NamedDomainObjectSet

/**
 * Defines a container which holds DSL objects.
 * Similar to a NamedDomainObjectContainer, but without its hardcoded configure methods.
 *
 * @param TSelf The type of the container itself.
 * @param TElement The type of the elements in the container.
 */
@CompileStatic
interface NamedDSLObjectContainer<TSelf extends NamedDSLObjectContainer<TSelf, TEntry>, TEntry extends BaseDSLElement<TEntry> & NamedDSLElement> extends NamedDomainObjectSet<TEntry>, BaseDSLElement<TSelf> {
    /**
     * Creates a new element in the container, with the given name.
     *
     * @param name The name of the element
     * @return The created element
     * @throws InvalidUserDataException When an object with the given name already exists.
     */
    TEntry create(String name) throws InvalidUserDataException;

    /**
     * Creates a new element in the container, with the given name.
     * If one already exists, it is returned instead.
     *
     * @param name The name of the element
     * @return The created or existing element
     */
    TEntry maybeCreate(String name);

    /**
     * Creates a new element in the container, with the given name, and configures it with the given action.
     *
     * @param name The name of the element
     * @param configureAction The action to configure the element with
     * @return The created element
     * @throws InvalidUserDataException When an object with the given name already exists.
     */
    TEntry create(String name, Action<? super TEntry> configureAction) throws InvalidUserDataException;

    /**
     * Creates a new element in the container, with the given name, and configures it with the given closure.
     *
     * @param name The name of the element
     * @param configurator The closure to configure the element with
     * @return The created element
     * @throws InvalidUserDataException When an object with the given name already exists.
     */
    TEntry create(String name, Closure configurator) throws InvalidUserDataException;

    /**
     * Creates a new element in the container, with the given name, and configures it with the given action.
     *
     * @param name The name of the element
     * @param configurationAction The action to configure the element with
     * @return A provider for the created element
     * @throws InvalidUserDataException When an object with the given name already exists.
     */
    NamedDomainObjectProvider<TEntry> register(String name, Action<? super TEntry> configurationAction) throws InvalidUserDataException;

    /**
     * Creates a new element in the container, with the given name, and configures it with the given action.
     *
     * @param name The name of the element
     * @return A provider for the created element
     * @throws InvalidUserDataException When an object with the given name already exists.
     */
    NamedDomainObjectProvider<TEntry> register(String name) throws InvalidUserDataException;
}