package net.neoforged.gradle.platform.tasks;

import com.google.gson.Gson;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.dsl.platform.util.CoordinateCollector;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

@CacheableTask
public abstract class CreateUserdevJson  extends DefaultRuntime implements WithOutput, WithWorkspace {
    
    public CreateUserdevJson() {
        getOutputFileName().set("config.json");
    }
    
    @TaskAction
    public void doTask() throws Exception {
        final Gson gson = UserdevProfile.createGson(getObjectFactory());
        final File output = ensureFileWorkspaceReady(getOutput());
        
        final UserdevProfile profile = getProfile().get();
        final UserdevProfile clone = gson.fromJson(gson.toJson(profile), UserdevProfile.class);
        
        final CoordinateCollector libraryCollector = new CoordinateCollector(getObjectFactory());
        getLibraries().getAsFileTree().visit(libraryCollector);
        clone.getAdditionalDependencyArtifactCoordinates().addAll(libraryCollector.getCoordinates());

        clone.getAdditionalTestDependencyArtifactCoordinates().addAll(getTestLibraries());

        final CoordinateCollector moduleCollector = new CoordinateCollector(getObjectFactory());
        getModules().getAsFileTree().visit(moduleCollector);
        clone.getModules().addAll(moduleCollector.getCoordinates());
        
        final String json = gson.toJson(clone);
        
        try {
            Files.write(output.toPath(), json.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    @Nested
    public abstract Property<UserdevProfile> getProfile();
    
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getLibraries();

    @Input
    public abstract ListProperty<String> getTestLibraries();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getModules();
}
