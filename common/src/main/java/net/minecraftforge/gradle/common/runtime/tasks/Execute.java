package net.minecraftforge.gradle.common.runtime.tasks;

import net.minecraftforge.gradle.common.util.TransformerUtils;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

@CacheableTask
public abstract class Execute extends Runtime implements net.minecraftforge.gradle.dsl.common.tasks.Execute {

    public Execute() {
        super();

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
    }

    @TaskAction
    @Override
    public void execute() throws Throwable {
        net.minecraftforge.gradle.dsl.common.tasks.Execute.super.execute();
    }

    @Input
    public abstract Property<String> getConsoleLogFileName();

    @Input
    public abstract Property<String> getLogFileName();

    @Input
    public abstract ListProperty<String> getProgramArguments();

    @Input
    public abstract Property<String> getExecutingArtifact();

    @Override
    public void buildRuntimeArguments(Map<String, Provider<String>> arguments) {
        super.buildRuntimeArguments(arguments);
        arguments.computeIfAbsent("log", k -> newProvider(getLogFile().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("console.log", k -> newProvider(getConsoleLogFile().get().getAsFile().getAbsolutePath()));
    }
}
