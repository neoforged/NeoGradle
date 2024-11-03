package net.neoforged.gradle.platform.extensions;

import net.neoforged.gradle.dsl.platform.extensions.LibraryManager;
import net.neoforged.gradle.dsl.platform.model.Library;
import net.neoforged.gradle.dsl.platform.util.LibraryCollector;
import net.neoforged.gradle.util.ModuleDependencyUtils;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class LibraryManagerImpl implements LibraryManager {

    private final Map<String, Provider<Set<String>>> classpathMap = new ConcurrentHashMap<>();

    private final Map<String, Provider<Set<Library>>> libraryMap = new ConcurrentHashMap<>();

    private final Project project;

    @Inject
    public LibraryManagerImpl(Project project) {
        this.project = project;
    }

    @Override
    public Provider<Set<String>> getClasspathOf(Provider<String> library) {
        return library.flatMap(lib -> classpathMap.computeIfAbsent(lib, key -> {
            final Provider<Configuration> configuration = getOrCreateConfigurationForTool(project, key);
            return gatherLibraryIdsFromConfiguration(project, configuration);
        }));
    }

    @Override
    public Provider<Set<Library>> getLibrariesOf(Provider<String> library) {
        return library.flatMap(lib -> libraryMap.computeIfAbsent(lib, key -> {
            final Provider<Configuration> configuration = getOrCreateConfigurationForTool(project, key);
            return gatherLibrariesFromConfiguration(project, configuration);
        }));
    }

    @Override
    public Provider<Set<String>> getClasspathOf(String library) {
        return classpathMap.computeIfAbsent(library, key -> {
            final Provider<Configuration> configuration = getOrCreateConfigurationForTool(project, key);
            return gatherLibraryIdsFromConfiguration(project, configuration);
        });
    }

    @Override
    public Provider<Set<Library>> getLibrariesOf(String library) {
        return libraryMap.computeIfAbsent(library, key -> {
            final Provider<Configuration> configuration = getOrCreateConfigurationForTool(project, key);
            return gatherLibrariesFromConfiguration(project, configuration);
        });
    }

    private static Provider<Set<Library>> gatherLibrariesFromConfiguration(Project project, Provider<Configuration> configurationProvider) {
        var repositoryUrls = project.getRepositories()
                .withType(MavenArtifactRepository.class).stream().map(MavenArtifactRepository::getUrl).collect(Collectors.toList());
        var logger = project.getLogger();

        var objectFactory = project.getObjects();

        // We use a property because it is *not* re-evaluated when queried, while a normal provider is
        var property = objectFactory.setProperty(Library.class);
        property.set(configurationProvider.flatMap(config -> {
            logger.info("Finding download URLs for configuration ${config.name}");
            return config.getIncoming().getArtifacts().getResolvedArtifacts().map(artifacts -> {
                var libraryCollector = new LibraryCollector(objectFactory, repositoryUrls, logger);

                for (ResolvedArtifactResult resolvedArtifact : artifacts) {
                    libraryCollector.visit(resolvedArtifact);
                }

                return libraryCollector.getLibraries();
            });
        }));
        property.finalizeValueOnRead();
        property.disallowChanges();
        return property;
    }

    private static Provider<Set<String>> gatherLibraryIdsFromConfiguration(Project project, Provider<Configuration> configurationProvider) {
        // We use a property because it is *not* re-evaluated when queried, while a normal provider is
        var property = project.getObjects().setProperty(String.class);
        var logger = project.getLogger();
        property.set(configurationProvider.flatMap(config ->
                config.getIncoming().getArtifacts().getResolvedArtifacts().map(artifacts ->
                        artifacts.stream().map(it -> {
                            var componentId = it.getId().getComponentIdentifier();
                            if (componentId instanceof ModuleComponentIdentifier moduleComponentIdentifier) {
                                var group = moduleComponentIdentifier.getGroup();
                                var module = moduleComponentIdentifier.getModule();
                                var version = moduleComponentIdentifier.getVersion();
                                var classifier = LibraryCollector.guessMavenClassifier(it.getFile(), moduleComponentIdentifier);
                                var extension = FilenameUtils.getExtension(it.getFile().getName());
                                if (classifier != "") {
                                    version += ":" + classifier;
                                }
                                return "%s:%s:%s@%s".formatted(group, module, version, extension);
                            } else {
                                logger.warn("Cannot handle component: {}", componentId);
                                return null;
                            }
                        }).filter(Objects::nonNull).collect(Collectors.toSet()))));
        property.finalizeValueOnRead();
        property.disallowChanges();
        return property;
    }

    private NamedDomainObjectProvider<Configuration> getOrCreateConfigurationForTool(Project project, String tool) {
        var configName = "neoForgeInstallerTool" + ModuleDependencyUtils.toConfigurationName(tool);
        try {
            return project.getConfigurations().named(configName);
        } catch (UnknownDomainObjectException ignored) {
            return project.getConfigurations().register(configName, (Configuration spec) -> {
                spec.setCanBeConsumed(false);
                spec.setCanBeResolved(true);
                spec.getDependencies().add(project.getDependencies().create(tool));
            });
        }
    }
}
