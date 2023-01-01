package net.minecraftforge.gradle.dsl.base

import groovy.transform.CompileStatic

@CompileStatic
interface NamedBaseDSLElement<TSelf extends NamedBaseDSLElement<TSelf>> extends BaseDSLElement<TSelf> {

    String getName();
}
