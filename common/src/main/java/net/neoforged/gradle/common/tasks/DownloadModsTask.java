package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.dsl.common.runs.run.DependencyHandler;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;

@DisableCachingByDefault(because = "This task downloads mods, which are not cacheable")
public abstract class DownloadModsTask extends NeoGradleBase {

    public DownloadModsTask() {
        getModsDirectory().convention(getRun().flatMap(run -> run.getWorkingDirectory().dir("mods")));

        //Ensure that the mods configuration is resolved after the project has been evaluated, during normal operation this is always the case, but a user could construct a task early
        ProjectUtils.afterEvaluate(getProject(), () -> getMods().from(getRun().flatMap(Run::getDependencies).map(DependencyHandler::getModsConfiguration)));
    }

    @TaskAction
    public void downloadMods() {
        final File modsDirectory = getModsDirectory().get().getAsFile();
        if (!modsDirectory.exists() && !modsDirectory.mkdirs()) {
            throw new RuntimeException("Could not create mods directory: " + modsDirectory);
        }

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(
            modsDirectory.toPath()
        );

        getMods().getAsFileTree().visit(visitor);
    }

    @Nested
    public abstract Property<Run> getRun();

    @OutputDirectory
    public abstract DirectoryProperty getModsDirectory();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getMods();
}
