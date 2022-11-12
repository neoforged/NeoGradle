package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.common.util.Artifact;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@CacheableTask
public abstract class DownloadArtifact extends Runtime {
    public DownloadArtifact() {
        getArtifact().convention(getArtifactCoordinate().map(Artifact::from));
        getArtifactCoordinate().finalizeValueOnRead();
    }

    @TaskAction
    public void doDownload() throws IOException {
        final File output = ensureFileWorkspaceReady(getOutput());

        final Artifact artifact = getArtifact().get();
        final Dependency dependency = artifact.toDependency(getProject());
        final Configuration dummy = getProject().getConfigurations().detachedConfiguration(dependency);
        dummy.setTransitive(false);
        final ResolvedConfiguration resolved = dummy.getResolvedConfiguration();
        final File resolvedArtifact = resolved.getFiles().iterator().next();

        Files.copy(resolvedArtifact.toPath(), output.toPath());
    }

    @Input
    @Optional
    public abstract Property<String> getArtifactCoordinate();

    @Input
    public abstract Property<Artifact> getArtifact();
}
