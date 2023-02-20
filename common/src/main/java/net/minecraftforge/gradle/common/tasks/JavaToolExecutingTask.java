package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.util.TransformerUtils;
import net.minecraftforge.gradle.dsl.common.tasks.Execute;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public abstract class JavaToolExecutingTask extends JavaRuntimeTask implements Execute {

    public JavaToolExecutingTask() {
        super();

        //All of these taskOutputs belong to the MCP group
        setGroup("forgegradle/runtimes");

        getArguments().convention(new HashMap<>());

        //And configure output default locations.
        getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir("forgegradle/tools/" + getName()));
        getOutputFileName().convention(getArguments().flatMap(arguments -> arguments.getOrDefault("outputExtension", getProject().provider(() -> "jar")).map(extension -> String.format("output.%s", extension))));
        getOutput().convention(getOutputDirectory().flatMap(d -> getOutputFileName().map(d::file)));

        getLogFileName().convention(getArguments().flatMap(arguments -> arguments.getOrDefault("log", getProject().provider(() -> "log.log"))));
        getLogFile().convention(getOutputDirectory().flatMap(d -> getLogFileName().map(d::file)));

        getConsoleLogFileName().convention(getArguments().flatMap(arguments -> arguments.getOrDefault("console.log", getProject().provider(() -> "console.log"))));
        getConsoleLogFile().convention(getOutputDirectory().flatMap(d -> getConsoleLogFileName().map(d::file)));

        getMainClass().convention(getExecutingJar().map(TransformerUtils.guardWithResource(
                jarFile -> jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS),
                f -> new JarFile(f.getAsFile())
        )));

        getExecutingJar().fileProvider(getExecutingArtifact().flatMap(artifact -> getDownloader().flatMap(downloader -> downloader.file(artifact))));

        getRuntimeProgramArguments().convention(getProgramArguments());

        //Configure the default runtime data map:
        getRuntimeArguments().convention(getArguments().map(arguments -> {
            final Map<String, Provider<String>> result = new HashMap<>(arguments);
            buildRuntimeArguments(result);
            return result;
        }));
        getRuntimeData().convention(new HashMap<>());
    }

    @TaskAction
    @Override
    public void execute() throws Throwable {
        Execute.super.execute();
    }

    /**
     * The arguments for this step.
     *
     * @return The arguments for this step.
     */
    @Input
    public abstract MapProperty<String, Provider<String>> getArguments();

    @Input
    public abstract Property<String> getConsoleLogFileName();

    @Input
    public abstract Property<String> getLogFileName();

    @Input
    public abstract ListProperty<String> getProgramArguments();

    @Input
    public abstract Property<String> getExecutingArtifact();

    protected void buildRuntimeArguments(final Map<String, Provider<String>> arguments) {
        arguments.computeIfAbsent("output", key -> newProvider(getOutput().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("outputDir", key -> newProvider(getOutputDirectory().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("javaVersion", key -> getJavaLauncher().map(launcher -> launcher.getMetadata().getLanguageVersion().toString()));
        arguments.computeIfAbsent("log", k -> newProvider(getLogFile().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("console.log", k -> newProvider(getConsoleLogFile().get().getAsFile().getAbsolutePath()));
    }
}
