package net.neoforged.gradle.common.runs.tasks;

import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "This runs a game. It should not be cached.")
public abstract class RunExec extends JavaExec {

    public static final String GROUP = "NeoGradle/Runs";

    public RunExec() {
        super();

        setGroup(GROUP);

        getMainClass().convention(getRun().flatMap(Run::getMainClass));

        doNotTrackState("This is a runnable task, which has no output.");
        
        JavaToolchainService service = getProject().getExtensions().getByType(JavaToolchainService.class);
        getJavaLauncher().convention(service.launcherFor(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain()));
    }

    @Override
    public void exec() {
        final RunImpl run = (RunImpl) getRun().get();

        setWorkingDir(run.getWorkingDirectory().get().getAsFile());
        args(run.getProgramArguments().get());
        jvmArgs(run.getJvmArguments().get());

        environment(run.getEnvironmentVariables().get());
        systemProperties(run.getSystemProperties().get());

        run.getModSources().all().get().values().stream()
                .map(SourceSet::getRuntimeClasspath)
                .forEach(this::classpath);

        classpath(run.getClasspath());

        classpath(run.getDependencies().get().getRuntimeConfiguration());

        super.exec();
    }

    @Nested
    public abstract Property<Run> getRun();
}
