package net.neoforged.gradle.platform.tasks;

import com.google.gson.Gson;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.dsl.platform.model.InstallerProfile;
import net.neoforged.gradle.dsl.platform.util.LibraryCollector;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

@CacheableTask
public abstract class CreateLegacyInstallerJson extends DefaultRuntime implements WithOutput, WithWorkspace {
    
    public CreateLegacyInstallerJson() {
        getOutputFileName().set("install_profile.json");
    }
    
    @TaskAction
    public void run() {
        final Gson gson = InstallerProfile.createGson(getObjectFactory());
        final File output = ensureFileWorkspaceReady(getOutput());
        
        final InstallerProfile profile = getProfile().get();
        final InstallerProfile copy = gson.fromJson(gson.toJson(profile), InstallerProfile.class);
        
        copy.getLibraries().addAll(
                getProviderFactory().provider(() -> {
                    final LibraryCollector profileFiller = new LibraryCollector(getObjectFactory());
                    getLibraries().getAsFileTree().visit(profileFiller);
                    return profileFiller.getLibraries();
                })
        );
        try {
            Files.write(output.toPath(), gson.toJson(copy).getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    @Nested
    public abstract Property<InstallerProfile> getProfile();
    
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getLibraries();
}
