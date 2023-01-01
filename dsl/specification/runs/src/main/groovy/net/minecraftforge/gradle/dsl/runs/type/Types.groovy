package net.minecraftforge.gradle.dsl.runs.type

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.base.util.NamedDSLObjectContainer

/**
 * Defines a DSL extension which allows for the registration of run types.
 */
@CompileStatic
interface Types extends NamedDSLObjectContainer<Types, Type> {
}