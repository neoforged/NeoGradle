package net.neoforged.gradle.common.util;

import net.neoforged.gradle.common.extensions.dependency.replacement.DependencyReplacementsExtension;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;

/**
 * Utility class for handling configurations
 */
public final class ConfigurationUtils {

    private ConfigurationUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ConfigurationUtils. This is a utility class");
    }

    /**
     * Creates a detached configuration that can be resolved, but not consumed.
     *
     * @param project The project to create the configuration for
     * @param dependencies The dependencies to add to the configuration
     * @return The detached configuration
     */
    public static Configuration temporaryConfiguration(final Project project, final Dependency... dependencies) {
        final Configuration configuration = project.getConfigurations().detachedConfiguration(dependencies);
        configuration.setCanBeConsumed(false);
        configuration.setCanBeResolved(true);

        final DependencyReplacement dependencyReplacement = project.getExtensions().getByType(DependencyReplacement.class);
        dependencyReplacement.handleConfiguration(configuration);

        return configuration;
    }
    
    /**
     * Creates a detached configuration that can be resolved, but not consumed.
     *
     * @param configurations The configuration handler.
     * @param dependencies The dependencies to add to the configuration
     * @return The detached configuration
     */
    public static Configuration temporaryUnhandledConfiguration(final ConfigurationContainer configurations, final Dependency... dependencies) {
        final Configuration configuration = configurations.detachedConfiguration(dependencies);
        configuration.setCanBeConsumed(false);
        configuration.setCanBeResolved(true);
        
        return configuration;
    }
}
