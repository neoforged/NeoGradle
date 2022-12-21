package net.minecraftforge.gradle.common.extensions.repository;

import com.google.common.collect.Sets;
import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.common.util.FileUtils;
import net.minecraftforge.gradle.dsl.common.extensions.repository.Repository;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public abstract class IvyDummyRepositoryExtension extends ConfigurableObject<IvyDummyRepositoryExtension> implements Repository<IvyDummyRepositoryExtension, IvyDummyRepositoryEntry, IvyDummyRepositoryEntry.Builder, IvyDummyRepositoryReference, IvyDummyRepositoryReference.Builder>
{
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
    private final Set<Consumer<Project>> entryConfigurators = Sets.newHashSet();
    private final Set<Consumer<Project>> afterEntryCallbacks = Sets.newHashSet();
    private boolean hasBeenRealized = false;

    private final Project project;

    @Inject
    public IvyDummyRepositoryExtension(Project project) {
        this.project = project;
        this.getRepositoryDirectory().convention(project.getLayout().getBuildDirectory().dir("libs"));
        this.createRepositories();
        this.getProject().afterEvaluate(p -> {
            this.hasBeenRealized = true;
            this.entryConfigurators.forEach(e -> e.accept(p));
            this.afterEntryCallbacks.forEach(e -> e.accept(p));
        });
    }

    @Override
    public Project getProject() {
        return project;
    }

    private void createRepositories() {
        project.getRepositories().ivy(repositoryConfiguration(
                "ForgeGradle Artifacts",
                getRepositoryDirectory()
        ));
    }

    @Override
    @NotNull
    public abstract DirectoryProperty getRepositoryDirectory();

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
            ivy.metadataSources(IvyArtifactRepository.MetadataSources::ivyDescriptor);
        };
    }

    @Override
    public void withDependency(final Action<IvyDummyRepositoryEntry.Builder> configurator, final Consumer<IvyDummyRepositoryEntry> configuredEntryConsumer) throws XMLStreamException, IOException {
        entryConfigurators.add(evaluatedProject -> {
            final IvyDummyRepositoryEntry.Builder builder = IvyDummyRepositoryEntry.Builder.create(getProject());
            configurator.execute(builder);
            final IvyDummyRepositoryEntry entry = builder.build();

            entries.add(builder.build());

            try {
                writeDummyDataIfNeeded(entry);
                configuredEntryConsumer.accept(entry);
            } catch (IOException | XMLStreamException e) {
                throw new RuntimeException("Failed to write dummy data for dependency: " + entry, e);
            }
        });
    }

    private void writeDummyDataIfNeeded(
            final IvyDummyRepositoryEntry entry
    ) throws IOException, XMLStreamException {
        final Path jarFile = entry.buildArtifactPath(getRepositoryDirectory().get().getAsFile().toPath());
        final Path baseDir = jarFile.getParent();
        final Path metaFile = baseDir.resolve(String.format("ivy-%s-fg%d.xml", entry.getVersion(), METADATA_VERSION));

        if (Files.exists(metaFile))
            return;

        Files.createDirectories(baseDir);
        final Path metaFileTmp = FileUtils.temporaryPath(metaFile.getParent(), "metadata");
        try (final IvyModuleWriter writer = new IvyModuleWriter(metaFileTmp)) {
            writer.write(entry);
        }
        FileUtils.atomicMove(metaFileTmp, metaFile);
        Files.createFile(jarFile);

        final Path sourcesFile = entry.asSources().buildArtifactPath(getRepositoryDirectory().get().getAsFile().toPath());
        Files.createFile(sourcesFile);
    }

    public Set<IvyDummyRepositoryEntry> getEntries() {
        return entries;
    }

    @Override
    public void afterEntryRealisation(Consumer<Project> projectConsumer) {
        if (this.hasBeenRealized) {
            projectConsumer.accept(this.getProject());
        } else {
            this.afterEntryCallbacks.add(projectConsumer);
        }
    }
}
