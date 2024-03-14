package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.dependency.DefaultDependencyFilter;
import net.neoforged.gradle.common.dependency.DefaultDependencyVersionInformationHandler;
import net.neoforged.gradle.common.dependency.JarJarArtifacts;
import net.neoforged.gradle.common.dependency.ResolvedJarJarArtifact;
import net.neoforged.gradle.common.manifest.DefaultInheritManifest;
import net.neoforged.gradle.common.manifest.InheritManifest;
import net.neoforged.gradle.dsl.common.dependency.DependencyFilter;
import net.neoforged.gradle.dsl.common.dependency.DependencyVersionInformationHandler;
import net.neoforged.jarjar.metadata.Metadata;
import net.neoforged.jarjar.metadata.MetadataIOHandler;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public abstract class JarJar extends Jar {

    private DependencyFilter dependencyFilter;
    private DependencyVersionInformationHandler dependencyVersionInformationHandler;

    @Nested
    abstract JarJarArtifacts getJarJarArtifacts();

    @Override
    public InheritManifest getManifest() {
        return (InheritManifest) super.getManifest();
    }

    private final CopySpec jarJarCopySpec;

    public JarJar() {
        this.jarJarCopySpec = this.getMainSpec().addChild();
        this.jarJarCopySpec.into("META-INF/jarjar");

        dependencyFilter = getProject().getObjects().newInstance(DefaultDependencyFilter.class);
        dependencyVersionInformationHandler = getProject().getObjects().newInstance(DefaultDependencyVersionInformationHandler.class);

        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE); //As opposed to shadow, we do not filter out our entries early!, So we need to handle them accordingly.
        setManifest(new DefaultInheritManifest(getServices().get(FileResolver.class)));
    }

    @Nested
    public DependencyFilter getDependencyFilter() {
        return dependencyFilter;
    }

    @Nested
    public DependencyVersionInformationHandler getDependencyVersionInformationHandler() {
        return dependencyVersionInformationHandler;
    }

    public JarJar dependencies(Action<DependencyFilter> c) {
        c.execute(dependencyFilter);
        return this;
    }

    public JarJar versionInformation(Action<DependencyVersionInformationHandler> c) {
        c.execute(dependencyVersionInformationHandler);
        return this;
    }

    @TaskAction
    @Override
    protected void copy() {
        List<ResolvedJarJarArtifact> includedJars = getJarJarArtifacts().getResolvedArtifacts().get();
        this.jarJarCopySpec.from(
                includedJars.stream().map(ResolvedJarJarArtifact::getFile).collect(Collectors.toList())
        );
        if (!writeMetadata(includedJars).jars().isEmpty()) {
            // Only copy metadata if not empty.
            this.jarJarCopySpec.from(getJarJarMetadataPath().toFile());
        }
        super.copy();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Metadata writeMetadata(List<ResolvedJarJarArtifact> includedJars) {
        final Path metadataPath = getJarJarMetadataPath();
        final Metadata metadata = createMetadata(includedJars);

        if (!metadata.jars().isEmpty()) {
            try {
                metadataPath.toFile().getParentFile().mkdirs();
                Files.deleteIfExists(metadataPath);
                Files.write(metadataPath, MetadataIOHandler.toLines(metadata), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write JarJar dependency metadata to disk.", e);
            }
        }
        return metadata;
    }

    public void configuration(Configuration jarJarConfiguration) {
        getJarJarArtifacts().configuration(jarJarConfiguration);
        dependsOn(jarJarConfiguration);
    }

    public void fromRuntimeConfiguration() {
        final Configuration runtimeConfiguration = getProject().getConfigurations().findByName("runtimeClasspath");
        if (runtimeConfiguration != null) {
            this.configuration(runtimeConfiguration);
        }
    }

    private Path getJarJarMetadataPath() {
        return getTemporaryDir().toPath().resolve("metadata.json");
    }

    private Metadata createMetadata(List<ResolvedJarJarArtifact> jars) {
        return new Metadata(
                jars.stream()
                        .map(ResolvedJarJarArtifact::createContainerMetadata)
                        .collect(Collectors.toList())
        );
    }
}
