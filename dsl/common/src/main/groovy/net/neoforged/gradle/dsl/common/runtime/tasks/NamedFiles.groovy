package net.neoforged.gradle.dsl.common.runtime.tasks

import groovy.transform.CompileStatic
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CompileStatic
class NamedFiles {

    private final String name;
    private final ConfigurableFileCollection files;

    NamedFiles(String name, ConfigurableFileCollection files) {
        this.name = name
        this.files = files
    }

    @Input
    String getName() {
        return name
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    ConfigurableFileCollection getFiles() {
        return files
    }
}
