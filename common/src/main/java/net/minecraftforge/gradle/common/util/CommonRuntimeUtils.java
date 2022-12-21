package net.minecraftforge.gradle.common.util;

import net.minecraftforge.gradle.common.runtime.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import org.apache.commons.lang3.StringUtils;
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

    public static Optional<TaskProvider<? extends WithOutput>> getInputTaskForTaskFrom(final CommonRuntimeSpec spec, final String inputValue, Map<String, TaskProvider<? extends WithOutput>> tasks) {
        Matcher matcher = OUTPUT_REPLACE_PATTERN.matcher(inputValue);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String stepName = matcher.group(1);
        if (stepName != null) {
            String taskName = CommonRuntimeUtils.buildTaskName(spec, stepName);
            switch (stepName) {
                case "downloadManifest":
                    taskName = NamingConstants.Task.CACHE_LAUNCHER_METADATA;
                    break;
                case "downloadJson":
                    taskName = NamingConstants.Task.CACHE_VERSION_MANIFEST;
                    break;
                case "downloadClient":
                    taskName = NamingConstants.Task.CACHE_VERSION_ARTIFACT_CLIENT;
                    break;
                case "downloadServer":
                    taskName = NamingConstants.Task.CACHE_VERSION_ARTIFACT_SERVER;
                    break;
                case "downloadClientMappings":
                    taskName = NamingConstants.Task.CACHE_VERSION_MAPPINGS_CLIENT;
                    break;
                case "downloadServerMappings":
                    taskName = NamingConstants.Task.CACHE_VERSION_MAPPINGS_SERVER;
                    break;
            }

            String finalTaskName = taskName;
            return Optional.ofNullable(tasks.get(finalTaskName));
        }

        return Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T extends CommonRuntimeDefinition<?>> Map<String, Provider<String>> buildArguments(final T definition, Map<String, String> values, final Map<String, TaskProvider<? extends WithOutput>> tasks, final Runtime taskForArguments, final Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        return buildArguments(value -> getInputTaskForTaskFrom(definition.spec(), value, tasks), value -> definition.spec().project().provider(() -> value), values, taskForArguments, alternativeInputProvider);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Map<String, Provider<String>> buildArguments(final Function<String, Optional<TaskProvider<? extends WithOutput>>> inputBuilder, final Function<String, Provider<String>> providerBuilder, Map<String, String> values, final Runtime taskForArguments, final Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        final Map<String, Provider<String>> arguments = new HashMap<>();

        values.forEach((key, value) -> {
            if (value.startsWith("{") && value.endsWith("}")) {
                Optional<TaskProvider<? extends WithOutput>> dependentTask;
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
