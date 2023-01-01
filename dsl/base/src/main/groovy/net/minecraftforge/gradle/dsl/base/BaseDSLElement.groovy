package net.minecraftforge.gradle.dsl.base

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.base.util.ConfigurableDSLElement
import net.minecraftforge.gradle.dsl.base.util.ProjectAssociatedDSLElement

@CompileStatic
interface BaseDSLElement<TSelf extends BaseDSLElement<TSelf>> extends ConfigurableDSLElement<TSelf>, ProjectAssociatedDSLElement {
}
