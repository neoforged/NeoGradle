package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.util.MinecraftArtifactType;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class MinecraftArtifactFileCacheProvider extends FileCacheProviding {
    
    public MinecraftArtifactFileCacheProvider() { }
    
    @TaskAction
    public void doRun() throws Throwable {
        getCentralCacheService().get()
                        .cached(
                                this,
                                ICacheableJob.Default.file(getOutput(), () -> doDownloadVersionDownloadToCache(
                                        getArtifactType().get().createIdentifier(getDistributionType().get()),
                                        String.format("Failed to download game artifact %s for %s", getArtifactType().get(), getDistributionType().get()),
                                        getManifest().get().getAsFile()
                                ))
                        ).execute();
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCentralCacheService();
    
    @Input
    public abstract Property<MinecraftArtifactType> getArtifactType();
    
    @Input
    public abstract Property<DistributionType> getDistributionType();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getManifest();
}
