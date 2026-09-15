package net.neoforged.gradle.common.extensions.base;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static net.neoforged.gradle.dsl.common.util.Constants.SUBSYSTEM_PROPERTY_PREFIX;

public abstract class WithPropertyLookup {
    protected final Project project;

    public WithPropertyLookup(Project project) {
        this.project = project;
    }

    protected Provider<String> getStringProperty(String propertyName, String defaultValue) {
        final Provider<String> property = this.getProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName);
        if (defaultValue == null)
            return property;

        return property.orElse(defaultValue);
    }

    protected Provider<Directory> getDirectoryProperty(String propertyName, Provider<Directory> defaultValue) {
        return this.getProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName)
                .flatMap(path -> project.getLayout().dir(project.provider(() -> new File(path))))
                .orElse(defaultValue);
    }

    protected Provider<Boolean> getBooleanProperty(String propertyName, boolean defaultValue, boolean disabledValue) {
        String fullPropertyName = SUBSYSTEM_PROPERTY_PREFIX + propertyName;
        return this.getProperty(fullPropertyName)
            .map(value -> {
                try {
                    return Boolean.valueOf(value);
                } catch (Exception e) {
                    throw new GradleException("Gradle Property " + fullPropertyName + " is not set to a boolean value: '" + value + "'");
                }
            }).orElse(defaultValue);
    }

    protected Provider<Boolean> getBooleanProperty(String propertyName) {
        String fullPropertyName = SUBSYSTEM_PROPERTY_PREFIX + propertyName;
        return this.getProperty(fullPropertyName)
            .map(value -> {
                try {
                    return Boolean.valueOf(value);
                } catch (Exception e) {
                    throw new GradleException("Gradle Property " + fullPropertyName + " is not set to a boolean value: '" + value + "'");
                }
            });
    }

    protected Provider<List<String>> getSpaceSeparatedListProperty(String propertyName, List<String> defaultValue) {
        return this.getProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName)
                .map(s -> Arrays.asList(s.split("\\s+")))
                .orElse(defaultValue);
    }

    private Provider<String> getProperty(String propertyName) {
        // Take a snapshot of the extra property at configuration as a fallback.
        // Extra properties are intended to be provided from other Gradle plugins that are
        // applied before this plugin to set default values programmatically.
        ExtraPropertiesExtension ext = this.project.getExtensions().getExtraProperties();
        String extValue;
        if (ext.has(propertyName)) {
            extValue = Objects.toString(ext.get(propertyName));
        } else {
            extValue = null;
        }

        Provider<String> gradleProperty = this.project.getProviders().gradleProperty(propertyName);
        Provider<String> extProperty = this.project.getProviders().provider(() -> extValue);

        return gradleProperty.orElse(extProperty);
    }

    public Project getProject() {
        return project;
    }
}
