package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.dsl.common.tasks.Execute;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

@CacheableTask
public abstract class DefaultExecute extends DefaultRuntime implements Execute {

    public DefaultExecute() {
        super();

        getLogFileName().convention(getArguments().getOrDefault("log", getProviderFactory().provider(() -> "log.log")).orElse("log.log"));
        getLogFile().convention(getOutputDirectory().flatMap(d -> getLogFileName().map(d::file)));

        getConsoleLogFileName().convention(getArguments().getOrDefault("console.log", getProviderFactory().provider(() -> "console.log")));
        getConsoleLogFile().convention(getOutputDirectory().flatMap(d -> getConsoleLogFileName().map(d::file)));

        getMainClass().convention(
            getExecutingClasspath().getElements()
                .map(e -> {
                    if (e.isEmpty())
                        return null;

                    return e.iterator().next();
                })
                .filter(fsl -> fsl != null && fsl.getAsFile().isFile())
                .map(TransformerUtils.guardWithResource(
                    jarFile -> jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS),
                    f -> new JarFile(f.getAsFile())
                ))
        );

        getRuntimeProgramArguments().convention(getProgramArguments());
        getMultiRuntimeArguments().convention(getMultiArguments().AsMap());

        getLogLevel().convention(LogLevel.ERROR);
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public void execute() throws Throwable {
        getCacheService().get()
                        .cached(
                                this,
                                ICacheableJob.Default.file(this::doExecute, getCacheableOutputs())
                        ).execute();
    }

    @Internal
    protected List<RegularFileProperty> getCacheableOutputs() {
        return new ArrayList<>(List.of(getOutput()));
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
