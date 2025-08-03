package net.neoforged.gradle.common.extensions.base;

import org.apache.commons.compress.utils.Lists;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.util.List;

public abstract class WithEnabledProperty extends WithLocalProperties
{

    public WithEnabledProperty(Project project, String prefix) {
        super(project, prefix);

        getIsEnabled().set(
                getBooleanLocalProperty("enabled", true)
        );
    };

    public WithEnabledProperty(WithEnabledProperty parent, String prefix) {
        super(parent, prefix);

        getIsEnabled().set(
                parent.getIsEnabled().zip(getBooleanLocalProperty("enabled", true), (parentEnabled, enabled) -> parentEnabled && enabled)
        );
    }

    public abstract Property<Boolean> getIsEnabled();


    @Override
    protected Provider<String> getStringProperty(String propertyName, String defaultValue)
    {
        return getIsEnabled().zip(
            getStringLocalProperty(propertyName, defaultValue),
            (enabled, value) -> enabled ? value : ""
        );
    }

    @Override
    protected Provider<Directory> getDirectoryProperty(String propertyName, Provider<Directory> defaultValue)
    {
        return getIsEnabled().zip(
            getDirectoryLocalProperty(propertyName, defaultValue),
            (enabled, value) -> enabled ? value : null
        );
    }

    @Override
    protected Provider<Boolean> getBooleanProperty(String propertyName, boolean defaultValue, boolean disabledValue)
    {
        return getIsEnabled().zip(
            getBooleanLocalProperty(propertyName, defaultValue),
            (enabled, value) -> enabled ? value : disabledValue
        );
    }

    @Override
    protected Provider<List<String>> getSpaceSeparatedListProperty(String propertyName, List<String> defaultValue)
    {
        return getIsEnabled().zip(
            getSpaceSeparatedListLocalProperty(propertyName, defaultValue),
            (enabled, value) -> enabled ? value : Lists.newArrayList()
        );
    }
}
