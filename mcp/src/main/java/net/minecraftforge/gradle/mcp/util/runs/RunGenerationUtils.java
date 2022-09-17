package net.minecraftforge.gradle.mcp.util.runs;

import net.minecraftforge.gradle.common.tasks.ExtractNatives;
import net.minecraftforge.gradle.common.util.RunConfig;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.common.util.VersionJson;
import net.minecraftforge.gradle.mcp.util.EclipseHacks;
import net.minecraftforge.gradle.mcp.util.runs.RunConfigGenerator;
import net.minecraftforge.gradle.mcp.extensions.McpMinecraftExtension;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RunGenerationUtils {

    private RunGenerationUtils() {
        throw new IllegalStateException("Tried to instantiate: 'RunGenerationUtils', but this is a utility class.");
    }

    public static void createRunConfigTasks(final McpMinecraftExtension extension, final TaskProvider<ExtractNatives> extractNatives, final TaskProvider<?>... setupTasks) {
        List<TaskProvider<?>> setupTasksLst = new ArrayList<>(Arrays.asList(setupTasks));

        final TaskProvider<Task> prepareRuns = extension.getProject().getTasks().register("prepareRuns", Task.class, task -> {
            task.setGroup(RunConfig.RUNS_GROUP);
            task.dependsOn(extractNatives, setupTasksLst);
        });

        final TaskProvider<Task> makeSrcDirs = extension.getProject().getTasks().register("makeSrcDirs", Task.class, task ->
                task.doFirst(t -> {
                    final JavaPluginExtension java = task.getProject().getExtensions().getByType(JavaPluginExtension.class);

                    java.getSourceSets().forEach(s -> s.getAllSource()
                            .getSrcDirs().stream().filter(f -> !f.exists()).forEach(File::mkdirs));
                }));
        setupTasksLst.add(makeSrcDirs);

        extension.getRuns().forEach(RunConfig::mergeParents);

        // Create run configurations _AFTER_ all projects have evaluated so that _ALL_ run configs exist and have been configured
        extension.getProject().getGradle().projectsEvaluated(gradle -> {
            VersionJson json = null;

            try {
                json = Utils.loadJson(extractNatives.get().getMeta().get().getAsFile(), VersionJson.class);
            } catch (IOException ignored) {
            }

            List<String> additionalClientArgs = json != null ? json.getPlatformJvmArgs() : Collections.emptyList();

            extension.getRuns().forEach(RunConfig::mergeChildren);
            extension.getRuns().forEach(run -> RunConfigGenerator.createRunTask(run, extension.getProject(), prepareRuns, additionalClientArgs));

            EclipseHacks.doEclipseFixes(extension, extractNatives, setupTasksLst);

            RunConfigGenerator.createIDEGenRunsTasks(extension, prepareRuns, makeSrcDirs, additionalClientArgs);
        });
    }
}
