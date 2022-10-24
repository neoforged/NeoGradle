package net.minecraftforge.gradle.mcp.util;

import net.minecraftforge.gradle.mcp.runtime.tasks.AccessTransformer;
import net.minecraftforge.gradle.mcp.runtime.tasks.FileCacheProviding;
import net.minecraftforge.gradle.mcp.runtime.tasks.IMcpRuntimeTask;
import net.minecraftforge.gradle.mcp.runtime.tasks.SideAnnotationStripper;
import net.minecraftforge.gradle.mcp.configuration.McpConfigConfigurationSpecV1;
import net.minecraftforge.gradle.mcp.runtime.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.runtime.spec.McpRuntimeSpec;
import net.minecraftforge.gradle.common.util.ICacheFileSelector;
import net.minecraftforge.gradle.common.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class McpRuntimeUtils {
    private static final Pattern OUTPUT_REPLACE_PATTERN = Pattern.compile("^\\{(\\w+)Output}$");

    private McpRuntimeUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: McpRuntimeUtils. This is a utility class");
    }

    public static String buildTaskName(final McpRuntimeSpec runtimeSpec, final String defaultName) {
        if (runtimeSpec.name().isEmpty())
            return defaultName;

        return runtimeSpec.name() + StringUtils.capitalize(defaultName);
    }

    public static String buildTaskName(final McpRuntimeDefinition runtimeSpec, final String defaultName) {
        return buildTaskName(runtimeSpec.spec(), defaultName);
    }

    public static String buildStepName(McpRuntimeSpec spec, String name) {
        return StringUtils.uncapitalize(name.replace(spec.name(), ""));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Provider<File> getTaskInputFor(final McpRuntimeSpec spec, final Map<String, TaskProvider<? extends IMcpRuntimeTask>> tasks, McpConfigConfigurationSpecV1.Step step, final String defaultInputTask, final Optional<TaskProvider<? extends IMcpRuntimeTask>> adaptedInput) {
        if (adaptedInput.isPresent()) {
            return adaptedInput.get().flatMap(task -> task.getOutput().getAsFile());
        }

        final String inputValue = step.getValue("input");
        if (inputValue == null) {
            return getInputForTaskFrom(spec, "{" + defaultInputTask + "Output}", tasks);
        }

        return getInputForTaskFrom(spec, inputValue, tasks);
    }

    public static Provider<File> getTaskInputFor(final McpRuntimeSpec spec, final Map<String, TaskProvider<? extends IMcpRuntimeTask>> tasks, McpConfigConfigurationSpecV1.Step step) {
        final String inputValue = step.getValue("input");
        if (inputValue == null) {
            throw new IllegalStateException("Can not transformer or get an input of a task without an input");
        }
        return getInputForTaskFrom(spec, inputValue, tasks);
    }

    public static Provider<File> getInputForTaskFrom(final McpRuntimeSpec spec, final String inputValue, Map<String, TaskProvider<? extends IMcpRuntimeTask>> tasks) {
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

    public static Optional<TaskProvider<? extends IMcpRuntimeTask>> getInputTaskForTaskFrom(final McpRuntimeSpec spec, final String inputValue, Map<String, TaskProvider<? extends IMcpRuntimeTask>> tasks) {
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

    public static TaskProvider<? extends AccessTransformer> createAccessTransformer(McpRuntimeSpec runtimeSpec, String namePreFix, List<File> files, Collection<String> data) {
        return runtimeSpec.project().getTasks().register(buildTaskName(runtimeSpec, String.format("apply%sAccessTransformer", Utils.capitalize(namePreFix))), AccessTransformer.class, task -> {
            task.getAdditionalTransformers().addAll(data);
            task.getTransformers().setFrom(runtimeSpec.configureProject().files(files.toArray()));
        });
    }

    /**
     * Internal Use Only
     * Non-Public API, Can be changed at any time.
     */
    public static TaskProvider<? extends SideAnnotationStripper> createSideAnnotationStripper(McpRuntimeSpec spec, String namePreFix, List<File> files, Collection<String> data) {
        return spec.project().getTasks().register(buildTaskName(spec, String.format("apply%sSideAnnotationStripper", Utils.capitalize(namePreFix))), SideAnnotationStripper.class, task -> {
            task.getAdditionalDataEntries().addAll(data);
            task.getDataFiles().setFrom(spec.configureProject().files(files.toArray()));
        });
    }

    @NotNull
    public static TaskProvider<FileCacheProviding> createFileCacheEntryProvidingTask(McpRuntimeSpec spec, String name, String outputFileName, File globalMinecraftCacheFile, ICacheFileSelector selector, String description) {
        return spec.project().getTasks().register(buildTaskName(spec, name), FileCacheProviding.class, task -> {
            task.getOutputFileName().set(outputFileName);
            task.getFileCache().set(globalMinecraftCacheFile);
            task.getSelector().set(selector);
            task.setDescription(description);
        });
    }
}
