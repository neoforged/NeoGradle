package net.neoforged.gradle.dsl.common.runtime.tasks

interface NamedFileRef {

    String getName()

    File getFile()
}