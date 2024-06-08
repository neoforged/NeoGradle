package net.neoforged.gradle.platform.tasks;

import com.google.gson.Gson;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.dsl.platform.util.CoordinateCollector;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CacheableTask
public abstract class CreateUserdevJson extends CreateJsonTask {

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

    @Nested
    public abstract Property<UserdevProfile> getProfile();

    @Input
    public abstract Property<ResolvedComponentResult> getLibraries();

    @Input
    public abstract Property<ResolvedComponentResult> getTestLibraries();

    @Input
    public abstract Property<ResolvedComponentResult> getModules();

    public static class IdExtractor
            implements Transformer<List<ComponentArtifactIdentifier>, Collection<ResolvedArtifactResult>> {
        @Override
        public List<ComponentArtifactIdentifier> transform(Collection<ResolvedArtifactResult> artifacts) {
            return artifacts.stream().map(ResolvedArtifactResult::getId).collect(Collectors.toList());
        }
    }
}
