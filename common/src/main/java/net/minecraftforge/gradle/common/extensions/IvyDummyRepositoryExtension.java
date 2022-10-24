package net.minecraftforge.gradle.common.extensions;

import com.google.common.collect.Sets;
import net.minecraftforge.gradle.common.repository.IvyDummyRepositoryEntry;
import net.minecraftforge.gradle.common.repository.IvyDummyRepositoryMetadataSupplier;
import net.minecraftforge.gradle.common.repository.IvyModuleWriter;
import net.minecraftforge.gradle.common.util.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

public abstract class IvyDummyRepositoryExtension {
    /**
     * A version for stored metadata.
     */
    public static int METADATA_VERSION = 2;

    /**
     * A variant of {@link IvyArtifactRepository#MAVEN_IVY_PATTERN} that takes
     * into account our metadata revision number.
     */
    public static final String IVY_METADATA_PATTERN = "[organisation]/[module]/[revision]/ivy-[revision]-fg" + METADATA_VERSION + ".xml";


    private final Set<IvyDummyRepositoryEntry> entries = Sets.newHashSet();

    private final Project project;

    @Inject
    public IvyDummyRepositoryExtension(Project project) {
        this.project = project;
        this.createRepositories();
    }


    private void createRepositories() {
        project.getRepositories().ivy(repositoryConfiguration(
                "ForgeGradle Artifacts",
                createRepoBaseDir()
        ));
    }

    @NotNull
    public Provider<Directory> createRepoBaseDir() {
        return project.getLayout().getBuildDirectory().dir("libs");
    }

    @SuppressWarnings("SameParameterValue") // Potentially this needs extension in the future.
    private Action<IvyArtifactRepository> repositoryConfiguration(
            final String name,
            final Provider<Directory> root
    ) {
        return ivy -> {
            ivy.setName(name);
            ivy.setUrl(root.get().getAsFile().toURI());
            ivy.patternLayout(layout -> {
                layout.artifact(IvyArtifactRepository.MAVEN_ARTIFACT_PATTERN);
                layout.ivy(IVY_METADATA_PATTERN);
                layout.setM2compatible(true);
            });
            ivy.setMetadataSupplier(IvyDummyRepositoryMetadataSupplier.class, params -> params.params(project.provider(this::getEntries), root));
            ivy.setAllowInsecureProtocol(true);
            ivy.getResolve().setDynamicMode(false);
            ivy.metadataSources(IvyArtifactRepository.MetadataSources::ivyDescriptor); //TODO HANDLE THIS
        };
    }

    public IvyDummyRepositoryEntry withDependency(final Action<IvyDummyRepositoryEntry.Builder> configurator) throws XMLStreamException, IOException {
        final IvyDummyRepositoryEntry.Builder builder = IvyDummyRepositoryEntry.Builder.create();
        configurator.execute(builder);
        final IvyDummyRepositoryEntry entry = builder.build();
        entries.add(builder.build());
        writeDummyDataIfNeeded(entry);
        return entry;
    }

    private void writeDummyDataIfNeeded(
            final IvyDummyRepositoryEntry entry
    ) throws IOException, XMLStreamException {
        final Path jarFile = entry.artifactPath(createRepoBaseDir().get().getAsFile().toPath());
        final Path baseDir = jarFile.getParent();
        final Path metaFile = baseDir.resolve(String.format("ivy-%s-fg%d.xml", entry.version(), METADATA_VERSION));

        if (Files.exists(metaFile))
            return;

        Files.createDirectories(baseDir);
        final Path metaFileTmp = FileUtils.temporaryPath(metaFile.getParent(), "metadata");
        try (final IvyModuleWriter writer = new IvyModuleWriter(metaFileTmp)) {
            writer.write(entry);
        }
        FileUtils.atomicMove(metaFileTmp, metaFile);
        Files.createFile(jarFile);

        final Path sourcesFile = entry.asSources().artifactPath(createRepoBaseDir().get().getAsFile().toPath());
        Files.createFile(sourcesFile);
    }

    public Collection<IvyDummyRepositoryEntry> getEntries() {
        return entries;
    }
}
