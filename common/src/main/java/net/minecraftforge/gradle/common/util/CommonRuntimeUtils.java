package net.minecraftforge.gradle.common.util;

import net.minecraftforge.gradle.common.runtime.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.common.runtime.tasks.FileCacheProviding;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommonRuntimeUtils {
    private static final Pattern OUTPUT_REPLACE_PATTERN = Pattern.compile("^\\{(\\w+)Output}$");

    private CommonRuntimeUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: McpRuntimeUtils. This is a utility class");
    }

    public static String buildTaskName(final TaskProvider<?> modifiedTask, final String defaultName) {
        return String.format("%s%s", defaultName, StringUtils.capitalize(modifiedTask.getName()));
    }


    public static String buildTaskName(final CommonRuntimeSpec runtimeSpec, final String defaultName) {
        if (runtimeSpec.name().isEmpty())
            return defaultName;

        return runtimeSpec.name() + StringUtils.capitalize(defaultName);
    }

    public static <D extends CommonRuntimeDefinition<?>> String buildTaskName(final D runtimeSpec, final String defaultName) {
        return buildTaskName(runtimeSpec.spec(), defaultName);
    }

    public static String buildStepName(CommonRuntimeSpec spec, String name) {
        return StringUtils.uncapitalize(name.replace(spec.name(), ""));
    }

    public static Provider<File> getInputForTaskFrom(final CommonRuntimeSpec spec, final String inputValue, Map<String, TaskProvider<? extends IRuntimeTask>> tasks) {
        Matcher matcher = OUTPUT_REPLACE_PATTERN.matcher(inputValue);
        if (!matcher.find()) {
            return spec.project().provider(() -> new File(inputValue));
        }

        String stepName = matcher.group(1);
        if (stepName != null) {
            return tasks.computeIfAbsent(buildTaskName(spec, stepName), value -> {
                throw new IllegalArgumentException("Could not find mcp task for input: " + value);
            }).flatMap(t -> t.getOutput().getAsFile());
        }

        throw new IllegalStateException("The string '" + inputValue + "' did not return a valid substitution match!");
    }

    public static Optional<TaskProvider<? extends IRuntimeTask>> getInputTaskForTaskFrom(final CommonRuntimeSpec spec, final String inputValue, Map<String, TaskProvider<? extends IRuntimeTask>> tasks) {
        Matcher matcher = OUTPUT_REPLACE_PATTERN.matcher(inputValue);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String stepName = matcher.group(1);
        if (stepName != null) {
            return Optional.ofNullable(tasks.get(buildTaskName(spec, stepName)));
        }

        return Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Map<String, Provider<String>> buildArguments(final CommonRuntimeSpec spec, Map<String, String> values, final Map<String, TaskProvider<? extends IRuntimeTask>> tasks, final IRuntimeTask taskForArguments, final Optional<TaskProvider<? extends IRuntimeTask>> alternativeInputProvider) {
        final Map<String, Provider<String>> arguments = new HashMap<>();

        values.forEach((key, value) -> {
            if (value.startsWith("{") && value.endsWith("}")) {
                Optional<TaskProvider<? extends IRuntimeTask>> dependentTask;
                if (!Objects.equals(key, "input") || !alternativeInputProvider.isPresent()) {
                    dependentTask = getInputTaskForTaskFrom(spec, value, tasks);
                } else {
                    dependentTask = alternativeInputProvider;
                }

                dependentTask.ifPresent(taskForArguments::dependsOn);
                dependentTask.ifPresent(task -> arguments.put(key, task.flatMap(t -> t.getOutput().getAsFile().map(File::getAbsolutePath))));
            } else {
                arguments.put(key, spec.project().provider(() -> value));
            }
        });

        return arguments;
    }


    @NotNull
    public static TaskProvider<FileCacheProviding> createFileCacheEntryProvidingTask(CommonRuntimeSpec spec, String name, String outputFileName, File globalMinecraftCacheFile, ICacheFileSelector selector, String description) {
        return spec.project().getTasks().register(buildTaskName(spec, name), FileCacheProviding.class, task -> {
            task.getOutputFileName().set(outputFileName);
            task.getFileCache().set(globalMinecraftCacheFile);
            task.getSelector().set(selector);
            task.setDescription(description);
        });
    }
}
