package net.minecraftforge.gradle.dsl.base

import net.minecraftforge.gradle.dsl.base.util.ConfigurableDSLElement

/**
 * Defines a base DSL object which is configurable and groovy extendable.
 *
 * @param <TSelf> The type of the DSL object,
 */
interface BaseDSLElement<TSelf extends BaseDSLElement<TSelf> & ConfigurableDSLElement<TSelf>> extends ConfigurableDSLElement<TSelf>, GroovyObject {
}