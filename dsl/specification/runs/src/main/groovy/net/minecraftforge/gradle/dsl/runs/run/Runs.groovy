package net.minecraftforge.gradle.dsl.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.base.util.NamedDSLObjectContainer

/**
 * Defines a DSL extension which allows for the registration of runs.
 */
@CompileStatic
interface Runs extends NamedDSLObjectContainer<Runs, Run> {
}