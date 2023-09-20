package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

@CacheableTask
public abstract class Execute extends DefaultRuntime implements net.neoforged.gradle.dsl.common.tasks.Execute {

    public Execute() {
        super();
        
        getLogFileName().convention(getArguments().flatMap(arguments -> arguments.getOrDefault("log", getProviderFactory().provider(() -> "log.log"))).orElse("log.log"));
        getLogFile().convention(getOutputDirectory().flatMap(d -> getLogFileName().map(d::file)));

        getConsoleLogFileName().convention(getArguments().flatMap(arguments -> arguments.getOrDefault("console.log", getProviderFactory().provider(() -> "console.log"))));
        getConsoleLogFile().convention(getOutputDirectory().flatMap(d -> getConsoleLogFileName().map(d::file)));

        getMainClass().convention(getExecutingJar().map(TransformerUtils.guardWithResource(
                jarFile -> jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS),
                f -> new JarFile(f.getAsFile())
        )));

        getRuntimeProgramArguments().convention(getProgramArguments());
        getMultiRuntimeArguments().convention(new HashMap<>());
    }

    @TaskAction
    @Override
    public void execute() throws Throwable {
        net.neoforged.gradle.dsl.common.tasks.Execute.super.execute();
    }

    @Input
    public abstract Property<String> getConsoleLogFileName();

    @Input
    public abstract Property<String> getLogFileName();

    @Input
    public abstract ListProperty<String> getProgramArguments();

    @Override
    public void buildRuntimeArguments(Map<String, Provider<String>> arguments) {
        super.buildRuntimeArguments(arguments);
        arguments.computeIfAbsent("log", k -> newProvider(getLogFile().get().getAsFile().getAbsolutePath()));
        arguments.computeIfAbsent("console.log", k -> newProvider(getConsoleLogFile().get().getAsFile().getAbsolutePath()));
    }
}
