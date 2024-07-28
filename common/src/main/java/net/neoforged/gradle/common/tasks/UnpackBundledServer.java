package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.util.BundledServerUtils;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class UnpackBundledServer extends NeoGradleBase implements WithOutput {

    @TaskAction
    public void doUnpack() throws Exception {
        final File serverJar = getServerJar().get().getAsFile();
        final File output = getOutput().get().getAsFile();
        
        if (!BundledServerUtils.isBundledServer(serverJar)) {
            FileUtils.copyFile(serverJar, output);
        } else {
            BundledServerUtils.extractBundledVersion(serverJar, output);
        }
    }
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getServerJar();
}
