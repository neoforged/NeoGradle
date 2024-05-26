package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems
import net.neoforged.gradle.dsl.common.runs.run.Run
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
class ConfigurationUtils {

    private static Set<Configuration> UNHANDLED_CONFIGURATIONS = new HashSet<>()

    private ConfigurationUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ConfigurationUtils. This is a utility class")
    }

    /**
     * Creates a configuration that can be resolved, but not consumed.
     *
     * @param project The project to create the configuration for
     * @param context The context of the configuration
     * @param dependencies The dependencies to add to the configuration
     * @return The detached configuration
     */
    static Configuration temporaryConfiguration(final Project project, final String context, final Dependency... dependencies) {
        final Configuration configuration = project.getConfigurations().maybeCreate("neoGradleInternal${context.capitalize()}")

        if (configuration.getDependencies().isEmpty()) {
            configuration.getDependencies().addAll(dependencies)

            configuration.setCanBeConsumed(false)
            configuration.setCanBeResolved(true)

            final DependencyReplacement dependencyReplacement = project.getExtensions().getByType(DependencyReplacement.class)
            dependencyReplacement.handleConfiguration(configuration)
        }

        return configuration
    }

    /**
     * Creates a configuration that can be resolved, but not consumed.
     *
     * @param project The project to create the configuration for
     * @param context The context of the configuration
     * @param processor The processor to apply to the configuration
     * @return The detached configuration
     */
    static Configuration temporaryConfiguration(final Project project, final String context, final Action<Configuration> processor) {
        final String name = "neoGradleInternal${context.capitalize()}"
        final boolean exists = project.getConfigurations().getNames().contains(name)

        final Configuration configuration = project.getConfigurations().maybeCreate("neoGradleInternal${context.capitalize()}")

        if (!exists) {
            processor.execute(configuration)

            configuration.setCanBeConsumed(false)
            configuration.setCanBeResolved(true)

            final DependencyReplacement dependencyReplacement = project.getExtensions().getByType(DependencyReplacement.class)
            dependencyReplacement.handleConfiguration(configuration)
        }

        return configuration
    }

    /**
     * Indicates if the given configuration is an unhandled configuration.
     *
     * @param configuration The configuration to check
     * @return True if the configuration is unhandled, false otherwise
     */
    public static boolean isUnhandledConfiguration(Configuration configuration) {
        return UNHANDLED_CONFIGURATIONS.contains(configuration)
    }

    /**
     * Creates a configuration that can be resolved, but not consumed, but on which no dependency replacement is applied.
     *
     * @param configurations The configuration handler.
     * @param context The context of the configuration
     * @param dependencies The dependencies to add to the configuration
     * @return The detached configuration
     */
    static Configuration temporaryUnhandledConfiguration(final ConfigurationContainer configurations, final String context, final Dependency... dependencies) {
        final Configuration configuration = configurations.maybeCreate("neoGradleInternalUnhandled${context.capitalize()}")
        UNHANDLED_CONFIGURATIONS.add(configuration)

        if (configuration.getDependencies().isEmpty()) {
            configuration.getDependencies().addAll(dependencies)

            configuration.setCanBeConsumed(false)
            configuration.setCanBeResolved(true)
        }

        return configuration
    }

    /**
     * Creates a configuration that can be resolved, but not consumed, and does not report its dependencies as transitive.
     *
     * @param configurations The configuration handler.
     * @param context The context of the configuration
     * @param dependencies The dependencies to add to the configuration
     * @return The detached configuration
     */
    static Configuration temporaryUnhandledNotTransitiveConfiguration(final ConfigurationContainer configurations, final String context, final Dependency... dependencies) {
        final Configuration configuration = configurations.maybeCreate("neoGradleInternalUnhandled${context.capitalize()}")
        UNHANDLED_CONFIGURATIONS.add(configuration)

        if (configuration.getDependencies().isEmpty()) {
            configuration.getDependencies().addAll(dependencies)

            configuration.setCanBeConsumed(false)
            configuration.setCanBeResolved(true)
            configuration.setTransitive(false)
        }

        return configuration
    }

    /**
     * Creates a provider that will resolve a temporary configuration containing the given dependency.
     */
    static Provider<File> getArtifactProvider(Project project, String context, Provider<? extends Object> dependencyNotationProvider) {
        return dependencyNotationProvider.flatMap(dependencyNotation -> {
            Configuration configuration = temporaryUnhandledNotTransitiveConfiguration(project.getConfigurations(), context, project.getDependencies().create(dependencyNotation));
            return configuration.getElements().map(files -> files.iterator().next().getAsFile());
        });
    }

    static List<Configuration> findReplacementConfigurations(final Project project, final Configuration configuration) {
        final Set<Configuration> resultContainer = new HashSet<>();

        resultContainer.addAll(findCompileOnlyConfigurationForSourceSetReplacement(project, configuration))
        resultContainer.addAll(findRuntimeOnlyConfigurationFromSourceSetReplacement(project, configuration))

        if (resultContainer.isEmpty()) {
            resultContainer.add(configuration)
        }

        return resultContainer.toList()
    }

    static List<Configuration> findCompileOnlyConfigurationForSourceSetReplacement(final Project project, final Configuration configuration) {
        final SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class)
        final List<Configuration> targets = new ArrayList<>();

        sourceSetContainer.forEach { sourceSet ->
            {
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
            }
        }

        return targets
    }

    static List<Configuration> findRuntimeOnlyConfigurationFromSourceSetReplacement(final Project project, final Configuration configuration) {
        final SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class)
        final List<Configuration> targets = new ArrayList<>();

        sourceSetContainer.forEach { sourceSet ->
            {
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
                    final Configuration reallyRuntimeOnly = project.getConfigurations().maybeCreate(getSourceSetName(
                            sourceSet,
                            project.getExtensions().getByType(Subsystems.class).getConventions().getConfigurations().getLocalRuntimeConfigurationPostFix().get()
                    ))
                    runtimeClasspath.extendsFrom(reallyRuntimeOnly)
                    targets.add(reallyRuntimeOnly)
                }
            }
        }

        return targets
    }

    static Set<Configuration> getAllSuperConfigurations(final Configuration configuration) {
        final Set<Configuration> supers = new HashSet<>();

        getAllSuperConfigurationsRecursive(configuration, supers)

        return supers
    }


    private static void getAllSuperConfigurationsRecursive(final Configuration configuration, final Set<Configuration> supers) {
        configuration.getExtendsFrom().forEach { config ->
            {
                if (supers.add(config)) {
                    getAllSuperConfigurationsRecursive(config, supers)
                }
            }
        }
    }

    /**
     * Gets the name of the source set with the given post fix
     *
     * @param sourceSet The source set to get the name of
     * @param postFix The post fix to append to the source set name
     * @return The name of the source set with the post fix
     */
    static String getSourceSetName(SourceSet sourceSet, String postFix) {
        final String capitalized = postFix.capitalize()
        final String name = sourceSet.getName() == SourceSet.MAIN_SOURCE_SET_NAME ? "" : sourceSet.getName().capitalize()

        return (name + capitalized).uncapitalize()
    }

    /**
     * Gets the name of the source set with the given post fix
     *
     * @param sourceSet The source set to get the name of
     * @param postFix The post fix to append to the source set name
     * @return The name of the source set with the post fix
     */
    static String getRunName(Run sourceSet, String postFix) {
        final String capitalized = postFix.capitalize()
        final String name = sourceSet.getName().capitalize()

        return (name + capitalized).uncapitalize()
    }
}
