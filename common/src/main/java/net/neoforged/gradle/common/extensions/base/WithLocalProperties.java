package net.neoforged.gradle.common.extensions.base;

import org.apache.commons.compress.utils.Lists;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;

import java.util.List;

public class WithLocalProperties extends WithPropertyLookup
{
    protected final String prefix;

    public WithLocalProperties(Project project, String prefix) {
        super(project);
        this.prefix = prefix;
    };

    public WithLocalProperties(WithEnabledProperty parent, String prefix) {
        super(parent.project);

        this.prefix = String.format("%s.%s", parent.prefix, prefix);
    }

    protected Provider<String> getStringLocalProperty(String propertyName, String defaultValue)
    {
        return super.getStringProperty(String.format("%s.%s", prefix, propertyName), defaultValue);
    }

    protected Provider<Directory> getDirectoryLocalProperty(String propertyName, Provider<Directory> defaultValue)
    {
        return super.getDirectoryProperty(String.format("%s.%s", prefix, propertyName), defaultValue);
    }

    protected Provider<Boolean> getBooleanLocalProperty(String propertyName, boolean defaultValue)
    {
        return super.getBooleanProperty(String.format("%s.%s", prefix, propertyName), defaultValue, false);
    }

    protected Provider<Boolean> getBooleanLocalProperty(String propertyName)
    {
        return super.getBooleanProperty(String.format("%s.%s", prefix, propertyName));
    }

    protected Provider<List<String>> getSpaceSeparatedListLocalProperty(String propertyName, List<String> defaultValue)
    {
        return super.getSpaceSeparatedListProperty(String.format("%s.%s", prefix, propertyName), defaultValue);
    }
}
