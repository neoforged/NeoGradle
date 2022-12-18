package net.minecraftforge.gradle.dsl.base

import net.minecraftforge.gradle.dsl.base.util.ProjectAssociatedDSLElement

interface ProjectAssociatedBaseDSLElement<TSelf extends ProjectAssociatedBaseDSLElement<TSelf>> extends BaseDSLElement<TSelf>, ProjectAssociatedDSLElement {
}
