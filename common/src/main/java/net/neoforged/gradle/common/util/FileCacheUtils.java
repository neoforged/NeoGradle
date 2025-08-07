package net.neoforged.gradle.common.util;

import net.neoforged.gradle.common.tasks.ExtractBundledServerTask;
import net.neoforged.gradle.common.tasks.MinecraftArtifactFileCacheProvider;
import net.neoforged.gradle.common.tasks.MinecraftLauncherFileCacheProvider;
import net.neoforged.gradle.common.tasks.MinecraftVersionManifestFileCacheProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.*;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Locale;

public final class FileCacheUtils {
    
    private FileCacheUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: FileCacheUtils. This is a utility class");
    }
    
    @NotNull
    public static TaskProvider<MinecraftLauncherFileCacheProvider> createLauncherMetadataFileCacheProvidingTask(final Project project) {
        if (project.getTasks().getNames().contains(NamingConstants.Task.CACHE_LAUNCHER_METADATA)) {
            return project.getTasks().named(NamingConstants.Task.CACHE_LAUNCHER_METADATA, MinecraftLauncherFileCacheProvider.class);
        }
        
        return project.getTasks().register(NamingConstants.Task.CACHE_LAUNCHER_METADATA, MinecraftLauncherFileCacheProvider.class, task -> {
        });
    }
    
    
    @NotNull
    public static TaskProvider<MinecraftVersionManifestFileCacheProvider> createVersionManifestFileCacheProvidingTask(final Project project, final MinecraftVersionAndUrl resolvedVersion) {
        if (project.getTasks().getNames().contains(NamingConstants.Task.CACHE_VERSION_MANIFEST + resolvedVersion.getVersion())) {
            return project.getTasks().named(NamingConstants.Task.CACHE_VERSION_MANIFEST + resolvedVersion.getVersion(), MinecraftVersionManifestFileCacheProvider.class);
        }

        return project.getTasks().register(NamingConstants.Task.CACHE_VERSION_MANIFEST + resolvedVersion.getVersion(), MinecraftVersionManifestFileCacheProvider.class, task -> {
            task.getMinecraftVersion().set(resolvedVersion.getVersion());
            task.getDownloadUrl().set(resolvedVersion.getUrl());
        });
    }
    
    @NotNull
    public static TaskProvider<MinecraftArtifactFileCacheProvider> createArtifactFileCacheProvidingTask(
        final Project project,
        final String minecraftVersion,
        final GameArtifact gameArtifact,
        final TaskProvider<MinecraftVersionManifestFileCacheProvider> versionManifestProvider)
    {
        final String taskName = NamingConstants.Task.CACHE_VERSION_PREFIX +
                                        StringUtils.capitalize(
                                                gameArtifact.getMinecraftArtifact().name().toLowerCase(Locale.ROOT)
                                        ) + StringUtils.capitalize(
                                                gameArtifact.getDistribution().getName().toLowerCase(Locale.ROOT)
                                        ) + minecraftVersion;
        
        if (project.getTasks().getNames().contains(taskName)) {
            return project.getTasks().named(taskName, MinecraftArtifactFileCacheProvider.class);
        }
        
        final CacheFileSelector selector = gameArtifact.getCacheSelectorForVersion(minecraftVersion);

        return project.getTasks().register(taskName, MinecraftArtifactFileCacheProvider.class, task -> {
            task.getManifest().set(versionManifestProvider.flatMap(WithOutput::getOutput));
            task.getArtifactType().set(gameArtifact);
            task.getSelector().set(selector);
        });
    }

    @NotNull
    public static TaskProvider<ExtractBundledServerTask> createExtractedServerFileCacheProvidingTask(
        final Project project,
        final String minecraftVersion,
        final GameArtifact gameArtifact,
        final TaskProvider<? extends WithOutput> bundledServerProvider)
    {
        final String taskName = NamingConstants.Task.CACHE_VERSION_PREFIX +
            StringUtils.capitalize(
                gameArtifact.getMinecraftArtifact().name().toLowerCase(Locale.ROOT)
            ) + StringUtils.capitalize(
            gameArtifact.getDistribution().getName().toLowerCase(Locale.ROOT)
        ) + minecraftVersion;

        if (project.getTasks().getNames().contains(taskName)) {
            return project.getTasks().named(taskName, ExtractBundledServerTask.class);
        }

        final CacheFileSelector selector = gameArtifact.getCacheSelectorForVersion(minecraftVersion);

        return project.getTasks().register(taskName, ExtractBundledServerTask.class, task -> {
            task.getSelector().set(selector);
            task.getMinecraftVersion().set(minecraftVersion);
            task.getInput().set(bundledServerProvider.flatMap(WithOutput::getOutput));
        });
    }
    
    @NotNull
    public static DirectoryProperty getAssetsCacheDirectory(Project project) {
        return project.getObjects().directoryProperty().fileValue(new File(project.getGradle().getGradleUserHomeDir(), "caches/minecraft/assets"));
    }
    
    @NotNull
    public static DirectoryProperty getLibrariesCacheDirectory(Project project) {
        return project.getObjects().directoryProperty().fileValue(new File(project.getGradle().getGradleUserHomeDir(), "caches/minecraft/libraries"));
    }
}
