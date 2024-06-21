package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.caching.CentralCacheService;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.dsl.common.util.MinecraftArtifactType;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;

import java.util.Arrays;

@CacheableTask
public abstract class MinecraftArtifactFileCacheProvider extends FileCacheProviding implements MinecraftGameArtifactProvidingTask {
    
    public MinecraftArtifactFileCacheProvider() { }
    
    @TaskAction
    public void doRun() throws Throwable {
        getCentralCacheService().get().doCached(
                this,
                () -> doDownloadVersionDownloadToCache(
                        getArtifactType().get().createIdentifier(getDistributionType().get()),
                        String.format("Failed to download game artifact %s for %s", getArtifactType().get(), getDistributionType().get()),
                        getManifest().get().getAsFile()
                ),
                getOutput()
        );
    }

    @ServiceReference(CommonProjectPlugin.EXECUTE_SERVICE)
    public abstract Property<CentralCacheService> getCentralCacheService();
    
    @Input
    public abstract Property<MinecraftArtifactType> getArtifactType();
    
    @Input
    public abstract Property<DistributionType> getDistributionType();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getManifest();

    @Override
    public GameArtifact gameArtifact() {
        return Arrays.stream(GameArtifact.values())
                .filter(artifact -> artifact.getDistributionType().map(distributionType -> distributionType.equals(getDistributionType().get())).orElse(false))
                .filter(artifact -> artifact.getType().map(artifactType -> artifactType.equals(getArtifactType().get())).orElse(false))
                .findFirst().orElseThrow();
    }
}
