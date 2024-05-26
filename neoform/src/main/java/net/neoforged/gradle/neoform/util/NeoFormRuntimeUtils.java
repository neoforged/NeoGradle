package net.neoforged.gradle.neoform.util;

import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV1;
import net.neoforged.gradle.neoform.runtime.specification.NeoFormRuntimeSpecification;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NeoFormRuntimeUtils {
    private static final Pattern OUTPUT_REPLACE_PATTERN = Pattern.compile("^\\{(\\w+)Output}$");

    private NeoFormRuntimeUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: NeoFormRuntimeUtils. This is a utility class");
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Provider<File> getTaskInputFor(final NeoFormRuntimeSpecification spec, final Map<String, TaskProvider<? extends WithOutput>> tasks, NeoFormConfigConfigurationSpecV1.Step step, final String defaultInputTask, final Optional<TaskProvider<? extends WithOutput>> adaptedInput, Task task) {
        if (adaptedInput.isPresent()) {
            task.dependsOn(adaptedInput);
            return adaptedInput.get().flatMap(t -> t.getOutput().getAsFile());
        }

        final String inputValue = step.getValue("input");
        if (inputValue == null) {
            return getInputForTaskFrom(spec, "{" + defaultInputTask + "Output}", tasks, task);
        }

        return getInputForTaskFrom(spec, inputValue, tasks, task);
    }

    public static Provider<File> getTaskInputFor(final NeoFormRuntimeSpecification spec, final Map<String, TaskProvider<? extends WithOutput>> tasks, NeoFormConfigConfigurationSpecV1.Step step, Task task) {
        final String inputValue = step.getValue("input");
        if (inputValue == null) {
            throw new IllegalStateException("Can not transformer or get an input of a task without an input");
        }
        return getInputForTaskFrom(spec, inputValue, tasks, task);
    }

    public static Provider<File> getInputForTaskFrom(final NeoFormRuntimeSpecification spec, final String inputValue, Map<String, TaskProvider<? extends WithOutput>> tasks, Task task) {
        Matcher matcher = OUTPUT_REPLACE_PATTERN.matcher(inputValue);
        if (!matcher.find()) {
            return spec.getProject().provider(() -> new File(inputValue));
        }

        String stepName = matcher.group(1);

        if (stepName != null) {
            String taskName = CommonRuntimeUtils.buildTaskName(spec, stepName);
            switch (stepName) {
                case "downloadManifest":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_LAUNCHER_METADATA, spec.getMinecraftVersion());
                    break;
                case "downloadJson":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_MANIFEST, spec.getMinecraftVersion());
                    break;
                case "downloadClient":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_ARTIFACT_CLIENT, spec.getMinecraftVersion());
                    break;
                case "downloadServer":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_ARTIFACT_SERVER, spec.getMinecraftVersion());
                    break;
                case "downloadClientMappings":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_MAPPINGS_CLIENT, spec.getMinecraftVersion());
                    break;
                case "downloadServerMappings":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_MAPPINGS_SERVER, spec.getMinecraftVersion());
                    break;
            }

            String finalTaskName = taskName;
            final TaskProvider<? extends WithOutput> inputTask = tasks.computeIfAbsent(taskName, value -> {
                throw new IllegalArgumentException("Could not find NeoForm task for input: " + value + ", available tasks: " + tasks.keySet() + ", input: " + inputValue + " taskname: " + finalTaskName + " stepname: " + stepName);
            });
            
            task.dependsOn(inputTask);
            
            return inputTask.flatMap(t -> t.getOutput().getAsFile());
        }

        throw new IllegalStateException("The string '" + inputValue + "' did not return a valid substitution match!");
    }

    public static Optional<TaskProvider<? extends WithOutput>> getInputTaskForTaskFrom(final NeoFormRuntimeSpecification spec, final String inputValue, Map<String, TaskProvider<? extends WithOutput>> tasks) {
        Matcher matcher = OUTPUT_REPLACE_PATTERN.matcher(inputValue);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String stepName = matcher.group(1);
        if (stepName != null) {
            String taskName = CommonRuntimeUtils.buildTaskName(spec, stepName);
            switch (stepName) {
                case "downloadManifest":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_LAUNCHER_METADATA, spec.getMinecraftVersion());
                    break;
                case "downloadJson":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_MANIFEST, spec.getMinecraftVersion());
                    break;
                case "downloadClient":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_ARTIFACT_CLIENT, spec.getMinecraftVersion());
                    break;
                case "downloadServer":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_ARTIFACT_SERVER, spec.getMinecraftVersion());
                    break;
                case "downloadClientMappings":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_MAPPINGS_CLIENT, spec.getMinecraftVersion());
                    break;
                case "downloadServerMappings":
                    taskName = String.format("%s%s", NamingConstants.Task.CACHE_VERSION_MAPPINGS_SERVER, spec.getMinecraftVersion());
                    break;
            }

            String finalTaskName = taskName;
            return Optional.ofNullable(tasks.get(finalTaskName));
        }

        return Optional.empty();
    }
    
    public static void configureDefaultRuntimeSpecBuilder(Project project, NeoFormRuntimeSpecification.Builder builder) {
        builder.withPostTaskAdapter("decompile", NeoFormAccessTransformerUtils.createAccessTransformerAdapter(project));
    }
}
