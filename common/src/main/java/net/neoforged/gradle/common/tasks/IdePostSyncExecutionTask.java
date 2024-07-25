package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;

public abstract class IdePostSyncExecutionTask extends NeoGradleBase {


    @InputFiles
    public abstract ConfigurableFileCollection getIdePostSyncFiles();
}
