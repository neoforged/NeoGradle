package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.util.MinecraftArtifactType;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class MinecraftArtifactFileCacheProvider extends FileCacheProviding {
    
    public MinecraftArtifactFileCacheProvider() { }
    
    @TaskAction
    public void doRun() throws Exception {
        doDownloadVersionDownloadToCache(
                getArtifactType().get().createIdentifier(getDistributionType().get()),
                String.format("Failed to download game artifact %s for %s", getArtifactType().get(), getDistributionType().get()),
                getManifest().get().getAsFile()
        );
    }
    
    @Input
    public abstract Property<MinecraftArtifactType> getArtifactType();
    
    @Input
    public abstract Property<DistributionType> getDistributionType();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getManifest();
}
