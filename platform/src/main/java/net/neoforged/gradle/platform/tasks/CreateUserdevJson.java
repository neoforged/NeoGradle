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
        
        collect(getLibraries(), clone.getAdditionalDependencyArtifactCoordinates());
        collect(getTestLibraries(), clone.getAdditionalTestDependencyArtifactCoordinates());
        collect(getModules(), clone.getModules());
        
        final String json = gson.toJson(clone);
        
        try {
            Files.write(output.toPath(), json.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void collect(ConfigurableFileCollection libraries, ListProperty<String> coords) {
        final CoordinateCollector collector = new CoordinateCollector(getObjectFactory());
        libraries.getAsFileTree().visit(collector);
        coords.addAll(collector.getCoordinates());
    }
    
    @Nested
    public abstract Property<UserdevProfile> getProfile();
    
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getLibraries();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getTestLibraries();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getModules();
}
