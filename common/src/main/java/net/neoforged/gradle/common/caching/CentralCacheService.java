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

public abstract class CentralCacheService implements BuildService<CentralCacheService.Params> {
    
    public static void register(Project project, String name, Provider<Directory> cacheDirectory) {
        project.getGradle().getSharedServices().registerIfAbsent(
                name,
                CentralCacheService.class,
                spec -> {
                    spec.getMaxParallelUsages().set(1);
                    spec.getParameters().getCacheDirectory().set(cacheDirectory);
                }
        );
    }

    @Internal
    @Override
    public abstract Params getParameters();
    
    public interface Params extends BuildServiceParameters {
        
        @Internal
        DirectoryProperty getCacheDirectory();
    }
}
