package net.neoforged.gradle.common.runs.tasks;

import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.util.ClasspathUtils;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
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

        if (run.getModSources().getPrimary().isPresent()) {
            final SourceSet primary = run.getModSources().getPrimary().get();

            final boolean primaryHasMinecraft = primary.getRuntimeClasspath().getFiles().stream().anyMatch(ClasspathUtils::isMinecraftClasspathEntry);

            //Remove any classpath entries that are already in the primary runtime classpath.
            //Also remove any classpath entries that are Minecraft, we can only have one Minecraft jar, in the case that the primary runtime classpath already has Minecraft.
            final FileCollection runtimeClasspathWithoutMinecraftAndWithoutPrimaryRuntimeClasspath =
                    this.classpath().getClasspath().filter(file -> !primary.getRuntimeClasspath().contains(file) && (!primaryHasMinecraft || !ClasspathUtils.isMinecraftClasspathEntry(file)));

            //Combine with the primary runtime classpath.
            final FileCollection combinedClasspath = primary.getRuntimeClasspath().plus(runtimeClasspathWithoutMinecraftAndWithoutPrimaryRuntimeClasspath);
            if (this.getClasspath() instanceof ConfigurableFileCollection classpath) {
                //Override
                classpath.setFrom(combinedClasspath);
            } else {
                throw new IllegalStateException("Classpath is not a ConfigurableFileCollection");
            }
        }

        super.exec();
    }

    @Nested
    public abstract Property<Run> getRun();
}
