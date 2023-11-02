package net.neoforged.gradle.common.util;

import net.neoforged.gradle.common.tasks.MinecraftArtifactFileCacheProvider;
import net.neoforged.gradle.common.tasks.MinecraftLauncherFileCacheProvider;
import net.neoforged.gradle.common.tasks.MinecraftVersionManifestFileCacheProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CacheFileSelector;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    public static TaskProvider<MinecraftVersionManifestFileCacheProvider> createVersionManifestFileCacheProvidingTask(final Project project, final String minecraftVersion, final TaskProvider<MinecraftLauncherFileCacheProvider> launcherProvider) {
        if (project.getTasks().getNames().contains(NamingConstants.Task.CACHE_VERSION_MANIFEST + minecraftVersion)) {
            return project.getTasks().named(NamingConstants.Task.CACHE_VERSION_MANIFEST + minecraftVersion, MinecraftVersionManifestFileCacheProvider.class);
        }
        
        return project.getTasks().register(NamingConstants.Task.CACHE_VERSION_MANIFEST + minecraftVersion, MinecraftVersionManifestFileCacheProvider.class, task -> {
            task.getMinecraftVersion().set(minecraftVersion);
            task.getLauncherManifest().set(launcherProvider.flatMap(WithOutput::getOutput));
        });
    }
    
    @NotNull
    public static TaskProvider<MinecraftArtifactFileCacheProvider> createArtifactFileCacheProvidingTask(final Project project, final String minecraftVersion, final DistributionType distributionType, final MinecraftArtifactType type, final TaskProvider<MinecraftVersionManifestFileCacheProvider> versionManifestProvider, final Collection<TaskProvider<? extends WithOutput>> otherProviders) {
        final String taskName = NamingConstants.Task.CACHE_VERSION_PREFIX +
                                        StringUtils.capitalize(
                                                type.name().toLowerCase()
                                        ) + StringUtils.capitalize(
                                                distributionType.getName().toLowerCase()
                                        ) + minecraftVersion;
        
        if (project.getTasks().getNames().contains(taskName)) {
            return project.getTasks().named(taskName, MinecraftArtifactFileCacheProvider.class);
        }
        
        final CacheFileSelector selector = type == MinecraftArtifactType.MAPPINGS ?
                                                   CacheFileSelector.forVersionMappings(minecraftVersion, distributionType.getName()) :
                                                   CacheFileSelector.forVersionJar(minecraftVersion, distributionType.getName());
        
        final List<TaskProvider<? extends WithOutput>> taskOrdering = new ArrayList<>(otherProviders);
        
        return project.getTasks().register(taskName, MinecraftArtifactFileCacheProvider.class, task -> {
            task.getDistributionType().set(distributionType);
            task.getManifest().set(versionManifestProvider.flatMap(WithOutput::getOutput));
            task.getArtifactType().set(type);
            task.getDistributionType().set(distributionType);
            task.getSelector().set(selector);
            
            taskOrdering.forEach(task::mustRunAfter);
        });
    }
    
    @NotNull
    public static DirectoryProperty getAssetsCacheDirectory(Project project) {
        return project.getObjects().directoryProperty().fileValue(new File(project.getGradle().getGradleUserHomeDir(), "caches/minecraft/assets"));
    }
}
