package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.common.util.TransformerUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class ForgeGradleBaseTask extends DefaultTask implements ITaskWithWorkspace {

    public ForgeGradleBaseTask() {
        setGroup("Forge Gradle");
    }




}
