package net.neoforged.gradle.common.util;

import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.util.internal.GUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ConfigurationUtils {
    private ConfigurationUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ConfigurationUtils. This is a utility class");
    }

    /**
     * Creates a configuration that can be resolved, but not consumed.
     *
     * @param project      The project to create the configuration for
     * @param context      The context of the configuration
     * @param dependencies The dependencies to add to the configuration
     * @return The detached configuration
     */
    public static Configuration temporaryConfiguration(final Project project, final String context, final Dependency... dependencies) {
        final Configuration configuration = project.getConfigurations().maybeCreate("neoGradleInternal" + StringGroovyMethods.capitalize(context));

        if (configuration.getDependencies().isEmpty()) {
            DefaultGroovyMethods.addAll(configuration.getDependencies(), dependencies);

            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(true);

            final DependencyReplacement dependencyReplacement = project.getExtensions().getByType(DependencyReplacement.class);
            dependencyReplacement.handleConfiguration(configuration);
        }


        return configuration;
    }

    /**
     * Creates a configuration that can be resolved, but not consumed.
     *
     * @param project   The project to create the configuration for
     * @param context   The context of the configuration
     * @param processor The processor to apply to the configuration
     * @return The detached configuration
     */
    public static Configuration temporaryConfiguration(final Project project, final String context, final Action<Configuration> processor) {
        final String name = "neoGradleInternal" + StringGroovyMethods.capitalize(context);
        final boolean exists = project.getConfigurations().getNames().contains(name);

        final Configuration configuration = project.getConfigurations().maybeCreate("neoGradleInternal" + StringGroovyMethods.capitalize(context));

        if (!exists) {
            processor.execute(configuration);

            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(true);

            final DependencyReplacement dependencyReplacement = project.getExtensions().getByType(DependencyReplacement.class);
            dependencyReplacement.handleConfiguration(configuration);
        }


        return configuration;
    }

    /**
     * Indicates if the given configuration is an unhandled configuration.
     *
     * @param configuration The configuration to check
     * @return True if the configuration is unhandled, false otherwise
     */
    public static boolean isUnhandledConfiguration(Configuration configuration) {
        return UNHANDLED_CONFIGURATIONS.contains(configuration);
    }

    /**
     * Creates a configuration that can be resolved, but not consumed, but on which no dependency replacement is applied.
     *
     * @param configurations The configuration handler.
     * @param context        The context of the configuration
     * @param dependencies   The dependencies to add to the configuration
     * @return The detached configuration
     */
    public static Configuration temporaryUnhandledConfiguration(final ConfigurationContainer configurations, final String context, final Dependency... dependencies) {
        final Configuration configuration = configurations.maybeCreate("neoGradleInternalUnhandled" + StringGroovyMethods.capitalize(context));
        UNHANDLED_CONFIGURATIONS.add(configuration);

        if (configuration.getDependencies().isEmpty()) {
            DefaultGroovyMethods.addAll(configuration.getDependencies(), dependencies);

            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(true);
        }


        return configuration;
    }

    /**
     * Creates a configuration that can be resolved, but not consumed, and does not report its dependencies as transitive.
     *
     * @param configurations The configuration handler.
     * @param context        The context of the configuration
     * @param dependencies   The dependencies to add to the configuration
     * @return The detached configuration
     */
    public static Configuration temporaryUnhandledNotTransitiveConfiguration(final ConfigurationContainer configurations, final String context, final Dependency... dependencies) {
        final Configuration configuration = configurations.maybeCreate("neoGradleInternalUnhandled" + StringGroovyMethods.capitalize(context));
        UNHANDLED_CONFIGURATIONS.add(configuration);

        if (configuration.getDependencies().isEmpty()) {
            DefaultGroovyMethods.addAll(configuration.getDependencies(), dependencies);

            configuration.setCanBeConsumed(false);
            configuration.setCanBeResolved(true);
            configuration.setTransitive(false);
        }


        return configuration;
    }

    /**
     * Creates a provider that will resolve a temporary configuration containing the given dependency.
     */
    public static Provider<File> getArtifactProvider(Project project, String context, Provider<? extends Object> dependencyNotationProvider) {
        return dependencyNotationProvider.flatMap(dependencyNotation -> {
            Configuration configuration = temporaryUnhandledNotTransitiveConfiguration(project.getConfigurations(), context, project.getDependencies().create(dependencyNotation));
            return configuration.getElements().map(files -> files.iterator().next().getAsFile());
        });

    }

    public static List<Configuration> findReplacementConfigurations(final Project project, final Configuration configuration) {
        final Set<Configuration> resultContainer = new HashSet<Configuration>();

        resultContainer.addAll(findCompileOnlyConfigurationForSourceSetReplacement(project, configuration));
        resultContainer.addAll(findRuntimeOnlyConfigurationFromSourceSetReplacement(project, configuration));

        if (resultContainer.isEmpty()) {
            resultContainer.add(configuration);
        }


        return new ArrayList<>(resultContainer);
    }

    public static List<Configuration> findConfigurationForSourceSetReplacement(final Project project, final Configuration configuration, final Function<SourceSet, Configuration> potentialTargetSelector, final boolean requiresSuperTarget) {
        final SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class);
        final List<Configuration> targets = new ArrayList<Configuration>();

        sourceSetContainer.forEach(sourceSet -> {
            final Configuration target = potentialTargetSelector.apply(sourceSet);
            final Configuration compileClasspath = project.getConfigurations().findByName(sourceSet.getCompileClasspathConfigurationName());

            if (configuration.equals(target)) {
                targets.clear();
                targets.add(target);
                return;
            }

            final Set<Configuration> supers = getAllSuperConfigurations(compileClasspath);
            if ((!requiresSuperTarget || supers.contains(target)) && supers.contains(configuration)) {
                targets.add(target);
            }
        });

        return targets;
    }

    public static List<Configuration> findSdkConfigurationForSourceSetReplacement(final Project project, final Configuration configuration) {
        return findConfigurationForSourceSetReplacement(project, configuration, ConfigurationUtils::getSdkConfiguration, false);
    }

    public static List<Configuration> findCompileOnlyConfigurationForSourceSetReplacement(final Project project, final Configuration configuration) {
        return findConfigurationForSourceSetReplacement(project, configuration, sourceSet -> project.getConfigurations().findByName(sourceSet.getCompileOnlyConfigurationName()), true);    }

    public static List<Configuration> findRuntimeOnlyConfigurationFromSourceSetReplacement(final Project project, final Configuration configuration) {
        final SourceSetContainer sourceSetContainer = project.getExtensions().getByType(SourceSetContainer.class);
        final List<Configuration> targets = new ArrayList<Configuration>();

        sourceSetContainer.forEach(sourceSet -> {
            final Configuration runtimeOnly = project.getConfigurations().findByName(sourceSet.getRuntimeOnlyConfigurationName());
            final Configuration runtimeClasspath = project.getConfigurations().findByName(sourceSet.getRuntimeClasspathConfigurationName());
            if (runtimeOnly == null) return;

            if (configuration.equals(runtimeOnly)) {
                targets.clear();
                targets.add(runtimeOnly);
                return;
            }

            final Set<Configuration> supers = getAllSuperConfigurations(runtimeClasspath);
            if (supers.contains(runtimeOnly) && supers.contains(configuration)) {
                final Configuration reallyRuntimeOnly = project.getConfigurations().maybeCreate(getSourceSetName(sourceSet, "neoGradleDependencyReplacementTarget%s".formatted(StringUtils.capitalize(configuration.getName()))));
                runtimeClasspath.extendsFrom(reallyRuntimeOnly);
                targets.add(reallyRuntimeOnly);
            }
        });

        return targets;
    }

    public static Set<Configuration> getAllSuperConfigurations(final Configuration configuration) {
        final Set<Configuration> supers = new HashSet<Configuration>();

        getAllSuperConfigurationsRecursive(configuration, supers);

        return supers;
    }

    private static void getAllSuperConfigurationsRecursive(final Configuration configuration, final Set<Configuration> supers) {
        configuration.getExtendsFrom().forEach(files -> {
            if (supers.add(files)) {
                getAllSuperConfigurationsRecursive(files, supers);
            }
        });
    }

    /**
     * Gets the name of the source set with the given post fix
     *
     * @param sourceSet The source set to get the name of
     * @param postFix   The post fix to append to the source set name
     * @return The name of the source set with the post fix
     */
    public static String getSourceSetName(SourceSet sourceSet, String postFix) {
        final String capitalized = StringGroovyMethods.capitalize(postFix);
        final String name = sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME) ? "" : StringGroovyMethods.capitalize(sourceSet.getName());

        return StringGroovyMethods.uncapitalize((name + capitalized));
    }

    /**
     * Gets the name of the source set with the given post fix
     *
     * @param sourceSet The source set to get the name of
     * @param postFix   The post fix to append to the source set name
     * @return The name of the source set with the post fix
     */
    public static String getRunName(Run sourceSet, String postFix) {
        final String capitalized = StringGroovyMethods.capitalize(postFix);
        final String name = StringGroovyMethods.capitalize(sourceSet.getName());

        return StringGroovyMethods.uncapitalize((name + capitalized));
    }

    public static String configurationNameOf(final SourceSet sourceSet, final String baseName) {
        return StringUtils.uncapitalize(getTaskBaseName(sourceSet) + StringUtils.capitalize(baseName));
    }

    public static Configuration getSdkConfiguration(final SourceSet sourceSet) {
        final Project project = SourceSetUtils.getProject(sourceSet);

        Configuration configuration = project.getConfigurations().findByName(configurationNameOf(sourceSet, "sdk"));
        if (configuration == null) {
            configuration = project.getConfigurations().create(configurationNameOf(sourceSet, "sdk"));
        }

        return configuration;
    }

    public static String getTaskBaseName(final SourceSet sourceSet) {
        return sourceSet.getName().equals(SourceSet.MAIN_SOURCE_SET_NAME) ? "" : GUtil.toCamelCase(sourceSet.getName());
    }

    private static Set<Configuration> UNHANDLED_CONFIGURATIONS = new HashSet<Configuration>();
}
