package net.neoforged.gradle.common.extensions.repository;

import com.google.common.collect.Sets;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryEntry;
import net.neoforged.gradle.dsl.common.extensions.repository.RepositoryReference;
import net.neoforged.gradle.dsl.common.util.ModuleReference;
import net.neoforged.gradle.util.FileUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class IvyDummyRepositoryExtension implements ConfigurableDSLElement<IvyDummyRepositoryExtension>, Repository<IvyDummyRepositoryExtension>
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
    private final LinkedHashSet<Consumer<Project>> entryConfigurators = new LinkedHashSet<>();
    private final Set<Consumer<Project>> afterEntryCallbacks = Sets.newHashSet();
    private final Set<ModuleReference> configuredReferences = Sets.newHashSet();
    private boolean hasBeenRealized = false;

    private final Project project;

    @Inject
    public IvyDummyRepositoryExtension(Project project) {
        this.project = project;
        this.getRepositoryDirectory().convention(project.getLayout().getProjectDirectory().dir(".gradle/repositories"));
        this.createRepositories();
    }

    @Override
    public Project getProject() {
        return project;
    }

    public void onPostDefinitionBake(final Project project) {
        this.hasBeenRealized = true;
        this.entryConfigurators.forEach(e -> e.accept(project));

        if (project.getState().getFailure() == null) {
            this.afterEntryCallbacks.forEach(e -> e.accept(project));
        }
    }

    private void createRepositories() {
        project.getRepositories().ivy(repositoryConfiguration(
                "NeoGradle Artifacts",
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
    public void withDependency(Action<RepositoryReference.Builder<?,?>> referenceBuilder, Action<RepositoryReference> onReferenceBuild, Action<RepositoryEntry.Builder<?,?,?>> configurator, Action<RepositoryEntry<?,?>> configuredEntryConsumer, boolean processImmediately) throws XMLStreamException, IOException {
        //Build the reference first so that a replacement dependency is potentially immediately available.
        final IvyDummyRepositoryEntry.Builder builder = project.getObjects().newInstance(IvyDummyRepositoryEntry.Builder.class, project);
        referenceBuilder.execute(builder);
        final IvyDummyRepositoryEntry entry = builder.build();

        onReferenceBuild.execute(entry);

        if (!processImmediately) {
            //Now schedule the on disk generation of the dependencies for after stuff is properly baked and evaluated.
            entryConfigurators.add(evaluatedProject -> {
                processDependency(configurator, configuredEntryConsumer);
            });
        }
        else {
            processDependency(configurator, configuredEntryConsumer);
        }
    }

    @Override
    public boolean isDynamicDependency(ModuleDependency dependency) {
        return dependency.getGroup() != null && dependency.getGroup().startsWith(IvyDummyRepositoryEntry.NG_DUMMY_NG_MARKER);
    }

    private void processDependency(Action<RepositoryEntry.Builder<?,?,?>> configurator, Action<RepositoryEntry<?,?>> configuredEntryConsumer) {
        final IvyDummyRepositoryEntry.Builder builder = IvyDummyRepositoryEntry.Builder.create(getProject());
        configurator.execute(builder);
        final IvyDummyRepositoryEntry entry = builder.build();

        processBuildEntry(configuredEntryConsumer, entry);
    }

    private void processBuildEntry(Action<RepositoryEntry<?,?>> configuredEntryConsumer, IvyDummyRepositoryEntry entry) {
        final ModuleReference reference = entry.toModuleReference();
        if (configuredReferences.contains(reference))
            return;

        configuredReferences.add(reference);

        registerEntry(entry);

        try {
            writeDummyDataIfNeeded(entry);
            configuredEntryConsumer.execute(entry);
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException("Failed to write dummy data for dependency: " + entry, e);
        }

        entry.getDependencies().stream().filter(IvyDummyRepositoryEntry.class::isInstance).map(IvyDummyRepositoryEntry.class::cast)
                .filter(e -> !e.toModuleReference().equals(entry.toModuleReference()))
                .forEach(e -> processBuildEntry(configuredEntryConsumer, e));
    }

    private void registerEntry(IvyDummyRepositoryEntry entry) {
        entries.add(entry);
    }

    private void writeDummyDataIfNeeded(
            final RepositoryEntry<?,?> entry
    ) throws IOException, XMLStreamException {
        final Path jarFile = entry.buildArtifactPath(getRepositoryDirectory().get().getAsFile().toPath());
        final Path baseDir = jarFile.getParent();
        final Path metaFile = baseDir.resolve(String.format("ivy-%s-fg%d.xml", entry.getVersion(), METADATA_VERSION));

        if (Files.exists(metaFile)) {
            FileUtils.delete(metaFile);
            writeIvyMetadataFile(entry, jarFile, baseDir, metaFile);
            return;
        }

        writeIvyMetadataFile(entry, jarFile, baseDir, metaFile);

        Files.createFile(jarFile);

        final Path sourcesFile = entry.asSources().buildArtifactPath(getRepositoryDirectory().get().getAsFile().toPath());
        Files.createFile(sourcesFile);
        writeDummyDependencyDataIfNeeded(entry);
    }

    private static void writeIvyMetadataFile(RepositoryEntry<?, ?> entry, Path jarFile, Path baseDir, Path metaFile) throws IOException, XMLStreamException {
        Files.createDirectories(baseDir);
        final Path metaFileTmp = FileUtils.temporaryPath(metaFile.getParent(), "metadata");
        try (final IvyModuleWriter writer = new IvyModuleWriter(metaFileTmp)) {
            writer.write(entry);
        }
        FileUtils.atomicMove(metaFileTmp, metaFile);
    }

    private void writeDummyDependencyDataIfNeeded(RepositoryEntry<?, ?> entry) throws IOException, XMLStreamException {
        for (RepositoryReference o : entry.getDependencies()) {
            if (o instanceof RepositoryEntry) {
                RepositoryEntry<?, ?> repositoryEntry = (RepositoryEntry<?, ?>) o;
                writeDummyDataIfNeeded(repositoryEntry);
            }
        }
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
