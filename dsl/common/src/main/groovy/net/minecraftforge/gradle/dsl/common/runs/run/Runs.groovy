package net.minecraftforge.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer

/**
 * Defines a DSL extension which allows for the registration of runs.
 */
@CompileStatic
interface Runs extends NamedDomainObjectContainer<Run> {
}