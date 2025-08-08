package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class MinecraftArtifactFileCacheProvider extends FileCacheProviding {
    
    public MinecraftArtifactFileCacheProvider() { }
    
    @TaskAction
    public void doRun() throws Throwable {
        final GameArtifact artifact = getArtifactType().get();

        getCentralCacheService().get()
                        .cached(
                                this,
                                ICacheableJob.Default.file(() -> doDownloadVersionDownloadToCache(
                                        artifact.getMinecraftArtifact().createIdentifier(artifact.getDistribution()),
                                        String.format("Failed to download game artifact %s for %s", getArtifactType().get(), artifact.getDistribution()),
                                        getManifest().get().getAsFile()
                                ), getOutput())
                        ).execute();
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCentralCacheService();
    
    @Input
    public abstract Property<GameArtifact> getArtifactType();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getManifest();
}
