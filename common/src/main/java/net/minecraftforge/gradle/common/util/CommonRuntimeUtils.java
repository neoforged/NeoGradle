package net.minecraftforge.gradle.common.util;

import net.minecraftforge.gradle.common.runtime.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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

    public static String buildTaskName(String name, ResolvedDependency resolvedDependency) {
        return String.format("%s%s", name, StringUtils.capitalize(resolvedDependency.getName()));
    }

    public static String buildTaskName(String name, String defaultName) {
        return String.format("%s%s", defaultName, StringUtils.capitalize(name));
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

    public static Optional<TaskProvider<? extends ITaskWithOutput>> getInputTaskForTaskFrom(final CommonRuntimeSpec spec, final String inputValue, Map<String, TaskProvider<? extends ITaskWithOutput>> tasks) {
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
    public static <T extends CommonRuntimeDefinition<?>> Map<String, Provider<String>> buildArguments(final T definition, Map<String, String> values, final Map<String, TaskProvider<? extends ITaskWithOutput>> tasks, final IRuntimeTask taskForArguments, final Optional<TaskProvider<? extends ITaskWithOutput>> alternativeInputProvider) {
        return buildArguments(value -> getInputTaskForTaskFrom(definition.spec(), value, tasks), value -> definition.spec().project().provider(() -> value), values, taskForArguments, alternativeInputProvider);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Map<String, Provider<String>> buildArguments(final Function<String, Optional<TaskProvider<? extends ITaskWithOutput>>> inputBuilder, final Function<String, Provider<String>> providerBuilder, Map<String, String> values, final IRuntimeTask taskForArguments, final Optional<TaskProvider<? extends ITaskWithOutput>> alternativeInputProvider) {
        final Map<String, Provider<String>> arguments = new HashMap<>();

        values.forEach((key, value) -> {
            if (value.startsWith("{") && value.endsWith("}")) {
                Optional<TaskProvider<? extends ITaskWithOutput>> dependentTask;
                if (!Objects.equals(key, "input") || !alternativeInputProvider.isPresent()) {
                    dependentTask = inputBuilder.apply(value);
                } else {
                    dependentTask = alternativeInputProvider;
                }

                dependentTask.ifPresent(taskForArguments::dependsOn);
                dependentTask.ifPresent(task -> arguments.put(key, task.flatMap(t -> t.getOutput().getAsFile().map(File::getAbsolutePath))));
            } else {
                arguments.put(key, providerBuilder.apply(value));
            }
        });

        return arguments;
    }
}
