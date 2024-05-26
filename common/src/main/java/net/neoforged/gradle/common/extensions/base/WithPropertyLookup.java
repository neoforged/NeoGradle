package net.neoforged.gradle.common.extensions.base;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.util.Arrays;
import java.util.List;

import static net.neoforged.gradle.dsl.common.util.Constants.SUBSYSTEM_PROPERTY_PREFIX;

public abstract class WithPropertyLookup {
    protected final Project project;

    public WithPropertyLookup(Project project) {
        this.project = project;
    }

    protected Provider<String> getStringProperty(String propertyName) {
        return this.project.getProviders().gradleProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName);
    }

    protected Provider<Boolean> getBooleanProperty(String propertyName) {
        String fullPropertyName = SUBSYSTEM_PROPERTY_PREFIX + propertyName;
        return this.project.getProviders().gradleProperty(fullPropertyName)
                .map(value -> {
                    try {
                        return Boolean.valueOf(value);
                    } catch (Exception e) {
                        throw new GradleException("Gradle Property " + fullPropertyName + " is not set to a boolean value: '" + value + "'");
                    }
                });
    }

    protected Provider<List<String>> getSpaceSeparatedListProperty(String propertyName) {
        return this.project.getProviders().gradleProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName)
                .map(s -> Arrays.asList(s.split("\\s+")));
    }

    public Project getProject() {
        return project;
    }
}
