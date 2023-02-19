package net.minecraftforge.gradle.common.util;

import net.minecraftforge.gradle.common.tasks.FileCacheProviding;
import net.minecraftforge.gradle.dsl.base.util.CacheFileSelector;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public final class FileCacheUtils {

    private FileCacheUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: FileCacheUtils. This is a utility class");
    }

    @SuppressWarnings("Convert2Lambda") // Task actions can not be lambdas.
    @NotNull
    public static TaskProvider<FileCacheProviding> createFileCacheEntryProvidingTask(final Project project, final String name, final String gameVersion, final File outputDirectory, final DirectoryProperty cacheDirectory, final CacheFileSelector selector, final Runnable action) {
        return project.getTasks().register(String.format("%s%s", name, gameVersion), FileCacheProviding.class, task -> {
            task.doFirst(new Action<Task>() {
                @Override
                public void execute(Task task) {
                    action.run();
                }
            });
            task.getOutputFileName().set(selector.getCacheFileName());
            task.getOutput().fileValue(new File(outputDirectory, selector.getCacheFileName()));
            task.getFileCache().set(cacheDirectory);
            task.getSelector().set(selector);
            task.setDescription("Retrieves: " + selector.getCacheFileName() + " from the central cache.");
        });
    }
}
