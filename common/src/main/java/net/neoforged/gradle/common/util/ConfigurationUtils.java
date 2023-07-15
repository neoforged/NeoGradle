package net.neoforged.gradle.common.util;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
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
        return configuration;
    }
}
