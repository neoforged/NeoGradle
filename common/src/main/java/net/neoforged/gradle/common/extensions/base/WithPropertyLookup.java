package net.neoforged.gradle.common.extensions.base;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static net.neoforged.gradle.dsl.common.util.Constants.SUBSYSTEM_PROPERTY_PREFIX;

public abstract class WithPropertyLookup {
    protected final Project project;

    public WithPropertyLookup(Project project) {
        this.project = project;
    }

    protected Provider<String> getStringProperty(String propertyName, String defaultValue) {
        final Provider<String> property = this.project.getProviders().gradleProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName);
        if (defaultValue == null)
            return property;

        return property.orElse(defaultValue);
    }

    protected Provider<Directory> getDirectoryProperty(String propertyName, Provider<Directory> defaultValue) {
        return this.project.getProviders().gradleProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName)
                .flatMap(path -> project.getLayout().dir(project.provider(() -> new File(path))))
                .orElse(defaultValue);
    }

    protected Provider<Boolean> getBooleanProperty(String propertyName, boolean defaultValue, boolean disabledValue) {
        String fullPropertyName = SUBSYSTEM_PROPERTY_PREFIX + propertyName;
        return this.project.getProviders().gradleProperty(fullPropertyName)
                .map(value -> {
                    try {
                        return Boolean.valueOf(value);
                    } catch (Exception e) {
                        throw new GradleException("Gradle Property " + fullPropertyName + " is not set to a boolean value: '" + value + "'");
                    }
                })
                .orElse(defaultValue);
    }

    protected Provider<List<String>> getSpaceSeparatedListProperty(String propertyName, List<String> defaultValue) {
        return this.project.getProviders().gradleProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName)
                .map(s -> Arrays.asList(s.split("\\s+")))
                .orElse(defaultValue);
    }

    public Project getProject() {
        return project;
    }
}
