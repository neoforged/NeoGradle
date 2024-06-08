package net.neoforged.gradle.platform.tasks;

import com.google.gson.Gson;
import net.neoforged.gradle.dsl.platform.model.InstallerProfile;
import net.neoforged.gradle.dsl.platform.util.LibrariesTransformer;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedVariantResult;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;

@CacheableTask
public abstract class CreateLegacyInstallerJson extends CreateJsonTask {
    
    public CreateLegacyInstallerJson() {
        getOutputFileName().set("install_profile.json");

        getLibraryArtifactIds().set(getLibraries().map(new LibrariesTransformer.IdExtractor()));
        getLibraryArtifactVariants().set(getLibraries().map(new LibrariesTransformer.VariantExtractor()));
        getLibraryArtifactFiles().set(getLibraries().map(new LibrariesTransformer.FileExtractor(getProject().getLayout())));
    }
    
    @TaskAction
    public void run() {
        final Gson gson = InstallerProfile.createGson(getObjectFactory());
        final File output = ensureFileWorkspaceReady(getOutput());
        
        final InstallerProfile profile = getProfile().get();
        final InstallerProfile copy = gson.fromJson(gson.toJson(profile), InstallerProfile.class);

        copy.getLibraries().addAll(
                LibrariesTransformer.transform(
                        getLibraryArtifactIds(),
                        getLibraryArtifactVariants(),
                        getLibraryArtifactFiles(),
                        getProject().getLayout(),
                        getObjectFactory()
                )
        );

        try {
            Files.write(output.toPath(), gson.toJson(copy).getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Nested
    public abstract Property<InstallerProfile> getProfile();

    @Internal
    public abstract SetProperty<ResolvedArtifactResult> getLibraries();

    @Input
    public abstract ListProperty<ComponentArtifactIdentifier> getLibraryArtifactIds();

    @Input
    public abstract ListProperty<ResolvedVariantResult> getLibraryArtifactVariants();

    @InputFiles
    @Classpath
    public abstract ListProperty<RegularFile> getLibraryArtifactFiles();

    @Input
    public abstract ListProperty<URI> getRepositoryURLs();


}
