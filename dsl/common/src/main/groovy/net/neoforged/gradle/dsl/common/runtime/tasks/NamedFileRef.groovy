package net.neoforged.gradle.dsl.common.runtime.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

interface NamedFileRef {

    @Input
    String getName()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    File getFile()
}