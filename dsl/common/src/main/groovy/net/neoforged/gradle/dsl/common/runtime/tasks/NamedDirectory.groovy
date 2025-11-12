package net.neoforged.gradle.dsl.common.runtime.tasks

import groovy.transform.CompileStatic
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

@CompileStatic
class NamedDirectory implements NamedFileRef {

    private final String name;
    private final Provider<Directory> file;

    NamedDirectory(String name, Provider<Directory> file) {
        this.name = name
        this.file = file
    }

    @Input
    String getName() {
        return name
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    Provider<File> getFile() {
        return file.map { it.asFile }
    }
}
