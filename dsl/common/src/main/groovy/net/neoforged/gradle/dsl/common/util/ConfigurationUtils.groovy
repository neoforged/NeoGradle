package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer

@CompileStatic
class ConfigurationUtils {

    private ConfigurationUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ConfigurationUtils. This is a utility class")
    }

    /**
     * Creates a detached configuration that can be resolved, but not consumed.
     *
     * @param project The project to create the configuration for
     * @param dependencies The dependencies to add to the configuration
     * @return The detached configuration
     */
    static Configuration temporaryConfiguration(final Project project, final Dependency... dependencies) {
        final Configuration configuration = project.getConfigurations().detachedConfiguration(dependencies)
        configuration.setCanBeConsumed(false)
        configuration.setCanBeResolved(true)

        final DependencyReplacement dependencyReplacement = project.getExtensions().getByType(DependencyReplacement.class)
        dependencyReplacement.handleConfiguration(configuration)

        return configuration
    }

    /**
     * Creates a detached configuration that can be resolved, but not consumed.
     *
     * @param configurations The configuration handler.
     * @param dependencies The dependencies to add to the configuration
     * @return The detached configuration
     */
    static Configuration temporaryUnhandledConfiguration(final ConfigurationContainer configurations, final Dependency... dependencies) {
        final Configuration configuration = configurations.detachedConfiguration(dependencies)
        configuration.setCanBeConsumed(false)
        configuration.setCanBeResolved(true)

        return configuration
    }

    /**
     * Creates a provider that will resolve a temporary configuration containing the given dependency.
     */
    static Provider<File> getArtifactProvider(Project project, Provider<? extends Object> dependencyNotationProvider) {
        return dependencyNotationProvider.flatMap(dependencyNotation -> {
            Configuration configuration = temporaryConfiguration(project, project.getDependencies().create(dependencyNotation));
            configuration.transitive = false;
            return configuration.getElements().map(files -> files.iterator().next().getAsFile());
        });
    }

    static List<Configuration> findReplacementConfigurations(final Project project, final Configuration configuration) {
        final Set<Configuration> resultContainer = new HashSet<>();

        resultContainer.addAll(findCompileOnlyConfigurationForSourceSetReplacement(project, configuration))
        resultContainer.addAll(findRuntimeOnlyConfigurationFromSourceSetReplacement(project, configuration))

        return resultContainer.toList()
    }

    static List<Configuration> findCompileOnlyConfigurationForSourceSetReplacement(final Project project, final Configuration configuration) {
        final SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class)
        final List<Configuration> targets = new ArrayList<>();

        sourceSetContainer.forEach {sourceSet -> {
            final Configuration compileOnly = project.getConfigurations().findByName(sourceSet.getCompileOnlyConfigurationName())
            final Configuration compileClasspath = project.getConfigurations().findByName(sourceSet.getCompileClasspathConfigurationName());
            if (compileOnly == null)
                return;

            if (configuration == compileOnly) {
                targets.clear()
                targets.add(compileOnly)
                return targets
            }

            final Set<Configuration> supers = getAllSuperConfigurations(compileClasspath)
            if (supers.contains(compileOnly) && supers.contains(configuration)) {
                targets.add(compileOnly)
            }
        }}

        return targets
    }

    static List<Configuration> findRuntimeOnlyConfigurationFromSourceSetReplacement(final Project project, final Configuration configuration) {
        final SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class)
        final List<Configuration> targets = new ArrayList<>();

        sourceSetContainer.forEach {sourceSet -> {
            final Configuration runtimeOnly = project.getConfigurations().findByName(sourceSet.getRuntimeOnlyConfigurationName())
            final Configuration runtimeClasspath = project.getConfigurations().findByName(sourceSet.getRuntimeClasspathConfigurationName());
            if (runtimeOnly == null)
                return;

            if (configuration == runtimeOnly) {
                targets.clear()
                targets.add(runtimeOnly)
                return targets
            }

            final Set<Configuration> supers = getAllSuperConfigurations(runtimeClasspath)
            if (supers.contains(runtimeOnly) && supers.contains(configuration)) {
                //Runtime is a special bunny, we need to make our own configuration in this state to handle it.
                //TODO: Once we add the conventions subsystem use its standardized approach.
                final Configuration reallyRuntimeOnly = project.getConfigurations().maybeCreate(
                    GradleInternalUtils.getSourceSetName(sourceSet, "runtimeNotPublished")
                )
                runtimeClasspath.extendsFrom(reallyRuntimeOnly)
                targets.add(reallyRuntimeOnly)
            }
        }}

        return targets
    }

    static Set<Configuration> getAllSuperConfigurations(final Configuration configuration) {
        final Set<Configuration> supers = new HashSet<>();

        getAllSuperConfigurationsRecursive(configuration, supers)

        return supers
    }


    private static void getAllSuperConfigurationsRecursive(final Configuration configuration, final Set<Configuration> supers) {
        configuration.getExtendsFrom().forEach {config -> {
            if (supers.add(config)) {
                getAllSuperConfigurationsRecursive(config, supers)
            }
        }}
    }
}
