package net.neoforged.gradle.common.extensions.repository;

import com.google.common.collect.Sets;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.repository.Entry;
import net.neoforged.gradle.dsl.common.extensions.repository.EntryDefinition;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.util.ModuleDependencyUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class IvyRepository implements ConfigurableDSLElement<Repository>, Repository
{
    /**
     * A version for stored metadata.
     */
    public static int METADATA_VERSION = 2;

    /**
     * A variant of {@link IvyArtifactRepository#MAVEN_IVY_PATTERN} that takes
     * into account our metadata revision number.
     */
    public static final String IVY_METADATA_PATTERN = "[organisation]/[module]/[revision]/ivy-[revision]-ng" + METADATA_VERSION + ".xml";

    private final Set<Entry> entries = Sets.newConcurrentHashSet();

    private final Project project;

    private ArtifactRepository gradleRepository;

    @Inject
    public IvyRepository(Project project) {
        this.project = project;
        this.getRepositoryDirectory().convention(project.getLayout().getProjectDirectory().dir(".gradle/repositories"));
        this.enable();
    }

    @Override
    public Project getProject() {
        return project;
    }

    private ArtifactRepository createRepositories() {
        return project.getRepositories().ivy(repositoryConfiguration(
                "NeoGradle Artifacts",
                getRepositoryDirectory()
        ));
    }

    @Override
    @NotNull
    public abstract DirectoryProperty getRepositoryDirectory();

    @Override
    public void enable() {
        this.gradleRepository = this.createRepositories();
    }

    @Override
    public void disable() {
        project.getRepositories().remove(gradleRepository);
    }

    @SuppressWarnings("SameParameterValue") // Potentially this needs extension in the future.
    private Action<IvyArtifactRepository> repositoryConfiguration(
            final String name,
            final Provider<Directory> root
    ) {
        //We primarily configure the metadata supplier here, but we also need to configure the repository itself.
        //We follow standard IVY patterns, and we also set M2 compatibility to true.
        return ivy -> {
            ivy.setName(name);
            ivy.setUrl(root.get().getAsFile().toURI());
            ivy.patternLayout(layout -> {
                layout.artifact(IvyArtifactRepository.MAVEN_ARTIFACT_PATTERN);
                layout.ivy(IVY_METADATA_PATTERN);
                layout.setM2compatible(true);
            });
            //We need to unpack our repo entries into pure serializable form.
            ivy.setMetadataSupplier(IvyMetadata.class, params -> params.params(project.provider(() -> entries.stream().map(IvyMetadata.MetadataEntry::from).collect(Collectors.toSet()))));
            ivy.setAllowInsecureProtocol(true);
            ivy.getResolve().setDynamicMode(false);
            ivy.metadataSources(IvyArtifactRepository.MetadataSources::ivyDescriptor);
        };
    }

    @Override
    public Entry withEntry(EntryDefinition entryDefinition) {
        //Let the definition create the entry.
        final Entry.Builder builder = project.getObjects().newInstance(IvyEntry.Builder.class, project);
        final Entry entry = entryDefinition.createFrom(builder);

        //Create and register the entry.
        create(entry);
        return entry;
    }

    @Override
    public RegularFileProperty createOutputFor(Entry entry, Variant variant) {
        final Path target = buildArtifactPath(entry.getDependency(), variant.adaptClassifier(
                ModuleDependencyUtils.getClassifierOrEmpty(entry.getDependency())
        ));

        return project.getObjects().fileProperty().fileValue(target.toFile());
    }

    @Override
    public RegularFileProperty createOutputFor(Dependency dependency, Variant variant) {
        final Path target = buildArtifactPath(dependency, variant.adaptClassifier(
                ModuleDependencyUtils.getClassifierOrEmpty(dependency)
        ));

        return project.getObjects().fileProperty().fileValue(target.toFile());
    }

    @Override
    public boolean isDynamicDependency(ModuleDependency dependency) {
        return entries.stream().anyMatch(entry -> entry.getDependency().equals(dependency));
    }

    @Override
    public Set<Entry> getEntries() {
        return Collections.unmodifiableSet(entries);
    }

    private void create(Entry entry) {
        final Dependency dependency = entry.getDependency();
        final Configuration dependencies = entry.getDependencies();
        final boolean hasSources = entry.hasSources();

        try {
            //Write the ivy.xml file if need be.
            writeDummyDataIfNeeded(dependency, dependencies, hasSources);
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException("Failed to write dummy data", e);
        }

        this.entries.add(entry);
    }

    private void writeDummyDataIfNeeded(
            final Dependency entry,
            final Configuration dependencies,
            final boolean hasSource
    ) throws IOException, XMLStreamException {
        //Construct all paths, ensuring that the metadata file name matches the pattern we configured above in the repo metadata.
        final Path jarFile = buildArtifactPath(entry);
        final Path baseDir = jarFile.getParent();
        final Path metaFile = baseDir.resolve(String.format("ivy-%s-ng%d.xml", entry.getVersion(), METADATA_VERSION));

        //Write the metadata file.
        writeIvyMetadataFile(entry, dependencies, baseDir, metaFile);

        //Create the raw artifact file and sources file if they don't exist.
        if (!Files.isRegularFile(jarFile)) {
            FileUtils.delete(jarFile);
            Files.createFile(jarFile);
        }

        if (hasSource) {
            final Path sourcesFile = buildArtifactPath(entry, "sources");
            if (!Files.isRegularFile(sourcesFile)) {
                FileUtils.delete(sourcesFile);
                Files.createFile(sourcesFile);
            }
        }
    }

    private static void writeIvyMetadataFile(Dependency entry, Configuration dependencies, Path baseDir, Path metaFile) throws IOException, XMLStreamException {
        Files.createDirectories(baseDir);

        //Write the metadata file to a temp target, then atomically move it to the final location.
        final Path metaFileTmp = FileUtils.temporaryPath(metaFile.getParent(), "metadata");
        try (final IvyModuleWriter writer = new IvyModuleWriter(metaFileTmp)) {
            writer.write(entry, dependencies);
        }
        FileUtils.atomicMove(metaFileTmp, metaFile);
    }

    public Path buildArtifactPath(Dependency dependency) {
        return getRepositoryDirectory().get().getAsFile().toPath().resolve(buildArtifactInnerPath(dependency));
    }

    public String buildArtifactInnerPath(Dependency dependency) {
        final String group = dependency.getGroup() == null ? "" : dependency.getGroup().replace('.', '/');
        final String name = dependency.getName();
        final String version = dependency.getVersion();
        final String classifier = ModuleDependencyUtils.getClassifierOrEmpty(dependency);
        final String extension = ModuleDependencyUtils.getExtensionOrJar(dependency);

        if (!classifier.isEmpty())
            return String.format("%s/%s/%s/%s-%s-%s.%s", group, name, version, name, version, classifier, extension);

        return String.format("%s/%s/%s/%s-%s.%s", group, name, version, name, version, extension);
    }

    public Path buildArtifactPath(Dependency dependency, String classifier) {
        return getRepositoryDirectory().get().getAsFile().toPath().resolve(buildArtifactInnerPath(dependency, classifier));
    }

    public String buildArtifactInnerPath(Dependency dependency, String classifier) {
        final String group = dependency.getGroup() == null ? "" : dependency.getGroup().replace('.', '/');
        final String name = dependency.getName();
        final String version = dependency.getVersion();
        final String extension = ModuleDependencyUtils.getExtensionOrJar(dependency);

        if (!classifier.isEmpty())
            return String.format("%s/%s/%s/%s-%s-%s.%s", group, name, version, name, version, classifier, extension);

        return String.format("%s/%s/%s/%s-%s.%s", group, name, version, name, version, extension);
    }
}
