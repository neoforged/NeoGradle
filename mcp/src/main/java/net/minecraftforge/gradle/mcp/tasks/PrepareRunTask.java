package net.minecraftforge.gradle.mcp.tasks;

import net.minecraftforge.gradle.common.tasks.ForgeGradleBaseTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Needs to run every time to configure a run configurations execution task.")
public abstract class PrepareRunTask extends ForgeGradleBaseTask {

    @Input
    public abstract Property<Boolean> getIsClientRun();

    @Input
    public abstract ListProperty<String> getAdditionalClientArguments();

    @Input
    public abstract ListProperty<String> getProgramArguments();

    @Input
    public abstract ListProperty<String> getJvmArguments();

    @Input
    public abstract MapProperty<String, Object> getEnvironmentVariables();

    @Input
    public abstract ListProperty<SourceSet> getSourceSets();

    @Input
    public abstract DirectoryProperty getWorkingDirectory();

    @Input
    public abstract Property<JavaExec> getExecutionTask();

    @TaskAction
    public void doRun() throws Exception {
        if (!getExecutionTask().isPresent()) {
            setDidWork(false);
            return;
        }

        final JavaExec exec = getExecutionTask().get();
        if (getProgramArguments().isPresent()) {
            exec.args(getProgramArguments().get());
        }
        if (getJvmArguments().isPresent()) {
            exec.jvmArgs(getJvmArguments().get());
        }

        if (getIsClientRun().getOrElse(false) && getAdditionalClientArguments().isPresent()) {
            exec.jvmArgs(getAdditionalClientArguments().get());
        }

        if (getEnvironmentVariables().isPresent()) {
            exec.setEnvironment(getEnvironmentVariables().get());
        }

        if (getSourceSets().isPresent()) {
            for (SourceSet sourceSet : getSourceSets().get()) {
                exec.classpath(sourceSet.getOutput());
            }
        }
    }
}
