package net.minecraftforge.gradle.common.util;

import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.File;
import java.io.Serializable;

public record FileWrapper(File file) implements Serializable {

    @Override
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public File file() {
        return this.file;
    }
}
