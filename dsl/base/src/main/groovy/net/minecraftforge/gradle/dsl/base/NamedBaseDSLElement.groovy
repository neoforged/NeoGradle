package net.minecraftforge.gradle.dsl.base

interface NamedBaseDSLElement<TSelf extends NamedBaseDSLElement<TSelf>> extends BaseDSLElement<TSelf> {

    String getName();
}
