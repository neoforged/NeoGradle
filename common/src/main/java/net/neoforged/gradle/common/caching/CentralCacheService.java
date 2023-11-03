package net.neoforged.gradle.common.caching;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;

/**
 * This is a cache service, which holds no directory information, yet.
 * I tried different ways of adding a directory property, but it always failed during isolation of the params.
 * For now please make sure that consuming tasks have a directory property, which is set to their cache directory.
 */
public abstract class CentralCacheService implements BuildService<BuildServiceParameters.None> {
    
    public static void register(Project project, String name, Provider<Directory> cacheDirectory) {
        project.getGradle().getSharedServices().registerIfAbsent(
                name,
                CentralCacheService.class,
                spec -> {
                    spec.getMaxParallelUsages().set(1);
                }
        );
    }
}
