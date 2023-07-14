package net.minecraftforge.gradle.dsl.common.util

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition
import net.minecraftforge.gradle.dsl.common.runtime.spec.Specification
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
final class CommonRuntimeUtils {
    private static final Pattern OUTPUT_REPLACE_PATTERN = Pattern.compile('^\\{(\\w+)Output}$');

    private CommonRuntimeUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: McpRuntimeUtils. This is a utility class");
    }

    static String buildTaskName(final TaskProvider<? extends Task> modifiedTask, final String defaultName) {
        return String.format("%s%s", defaultName, StringUtils.capitalize(modifiedTask.getName()));
    }

    static String buildTaskName(final Specification runtimeSpec, final String defaultName) {
        if (runtimeSpec.getName().isEmpty())
            return defaultName;

        return runtimeSpec.getName() + StringUtils.capitalize(defaultName);
    }

    static <D extends Definition<? extends Specification>> String buildTaskName(final D runtimeSpec, final String defaultName) {
        return buildTaskName(runtimeSpec.getSpecification(), defaultName);
    }

    static String buildTaskName(String name, ResolvedDependency resolvedDependency) {
        final String dependencyName = StringUtils.capitalize(resolvedDependency.getName());
        final String validDependencyName = dependencyName.replaceAll("[/\\\\:<>\"?*|]", "_");

        final String validName = name.replaceAll("[/\\\\:<>\"?*|]", "_");

        return String.format("%s%s", validName, validDependencyName);
    }

    static String buildTaskName(String name, String defaultName) {
        final String suffix = StringUtils.capitalize(name);
        final String validSuffix = suffix.replaceAll("[/\\\\:<>\"?*|]", "_");

        final String validName = defaultName.replaceAll("[/\\\\:<>\"?*|]", "_");

        return String.format("%s%s", validName, validSuffix);
    }

    static String buildTaskName(String prefix, ModuleReference reference) {
        final String dependencyName = StringUtils.capitalize(reference.toString());
        final String validDependencyName = dependencyName.replaceAll("[/\\\\:<>\"?*|]", "_");

        final String validPrefix = prefix.replaceAll("[/\\\\:<>\"?*|]", "_");

        return String.format("%s%s", validPrefix, validDependencyName);
    }

    static String buildStepName(Specification spec, String name) {
        return StringUtils.uncapitalize(name.replace(spec.getName(), ""));
    }

    static Optional<TaskProvider<? extends WithOutput>> getInputTaskForTaskFrom(final Specification spec, final String inputValue, Map<String, TaskProvider<? extends WithOutput>> tasks) {
        Matcher matcher = OUTPUT_REPLACE_PATTERN.matcher(inputValue);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String stepName = matcher.group(1);
        if (stepName != null) {
            String taskName = buildTaskName(spec, stepName);
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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T extends Definition<? extends Specification>> Map<String, Provider<String>> buildArguments(final T definition, Map<String, String> values, final Map<String, TaskProvider<? extends WithOutput>> tasks, final Runtime taskForArguments, final Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        return buildArguments(value -> getInputTaskForTaskFrom(definition.getSpecification(), value, tasks), value -> definition.getSpecification().getProject().provider(() -> value), values, taskForArguments, alternativeInputProvider);
    }

    @CompileDynamic
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static Map<String, Provider<String>> buildArguments(final Function<String, Optional<TaskProvider<? extends WithOutput>>> inputBuilder, final Function<String, Provider<String>> providerBuilder, Map<String, String> values, final Runtime taskForArguments, final Optional<TaskProvider<? extends WithOutput>> alternativeInputProvider) {
        final Map<String, Provider<String>> arguments = new HashMap<>();

        for (final String key in values.keySet()) {
            final String value = values.get(key);
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
        }

        return arguments;
    }
}
