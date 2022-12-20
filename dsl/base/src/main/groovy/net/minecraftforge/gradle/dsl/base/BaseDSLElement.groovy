package net.minecraftforge.gradle.dsl.base

import net.minecraftforge.gradle.dsl.base.util.ConfigurableDSLElement
import net.minecraftforge.gradle.dsl.base.util.ProjectAssociatedDSLElement

interface BaseDSLElement<TSelf extends BaseDSLElement<TSelf>> extends ConfigurableDSLElement<TSelf>, ProjectAssociatedDSLElement {
}
