package net.neoforged.gradle.common.extensions.base;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.util.List;

public abstract class WithEnabledProperty extends WithPropertyLookup {

    private final String prefix;

    public WithEnabledProperty(Project project, String prefix) {
        super(project);
        this.prefix = prefix;

        getIsEnabled().set(
                getBooleanLocalProperty("enabled", true)
        );
    };

    public WithEnabledProperty(WithEnabledProperty parent, String prefix) {
        super(parent.project);

        this.prefix = String.format("%s.%s", parent.prefix, prefix);
        getIsEnabled().set(
                parent.getIsEnabled().zip(getBooleanLocalProperty("enabled", true), (parentEnabled, enabled) -> parentEnabled && enabled)
        );
    }

    public abstract Property<Boolean> getIsEnabled();

    protected Provider<String> getStringLocalProperty(String propertyName, String defaultValue) {
        return super.getStringProperty(String.format("%s.%s", prefix, propertyName), defaultValue);
    }

    protected Provider<Directory> getDirectoryLocalProperty(String propertyName, Provider<Directory> defaultValue) {
        return super.getDirectoryProperty(String.format("%s.%s", prefix, propertyName), defaultValue);
    }

    protected Provider<Boolean> getBooleanLocalProperty(String propertyName, boolean defaultValue) {
        return super.getBooleanProperty(String.format("%s.%s", prefix, propertyName), defaultValue, false);
    }

    protected Provider<List<String>> getSpaceSeparatedListLocalProperty(String propertyName, List<String> defaultValue) {
        return super.getSpaceSeparatedListProperty(String.format("%s.%s", prefix, propertyName), defaultValue);
    }

    @Override
    protected Provider<String> getStringProperty(String propertyName, String defaultValue) {
        return getIsEnabled().zip(
                getStringLocalProperty(propertyName, defaultValue),
                (enabled, value) -> enabled ? value : ""
        );
    }

    @Override
    protected Provider<Directory> getDirectoryProperty(String propertyName, Provider<Directory> defaultValue) {
        return getIsEnabled().zip(
                getDirectoryLocalProperty(propertyName, defaultValue),
                (enabled, value) -> enabled ? value : null
        );
    }

    @Override
    protected Provider<Boolean> getBooleanProperty(String propertyName, boolean defaultValue, boolean disabledValue) {
        return getIsEnabled().zip(
                getBooleanLocalProperty(propertyName, defaultValue),
                (enabled, value) -> enabled ? value : disabledValue
        );
    }

    @Override
    protected Provider<List<String>> getSpaceSeparatedListProperty(String propertyName, List<String> defaultValue) {
        return getIsEnabled().zip(
                getSpaceSeparatedListLocalProperty(propertyName, defaultValue),
                (enabled, value) -> enabled ? value : List.of()
        );
    }
}
