package net.neoforged.gradle.common.extensions.base;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.util.List;

public abstract class WithEnabledProperty extends WithPropertyLookup {

    private final String prefix;

    public WithEnabledProperty(Project project, String prefix) {
        super(project);
        this.prefix = prefix;

        getIsEnabled().convention(
                getBooleanLocalProperty("enabled").orElse(true)
        );
    };

    public WithEnabledProperty(WithEnabledProperty parent, String prefix) {
        super(parent.project);

        this.prefix = String.format("%s.%s", parent.prefix, prefix);
        getIsEnabled().convention(
                parent.getIsEnabled().zip(getBooleanLocalProperty("enabled"), (parentEnabled, enabled) -> parentEnabled && enabled).orElse(true)
        );
    }

    public abstract Property<Boolean> getIsEnabled();

    private Provider<String> getStringLocalProperty(String propertyName) {
        return super.getStringProperty(String.format("%s.%s", prefix, propertyName));
    }

    private Provider<Boolean> getBooleanLocalProperty(String propertyName) {
        return super.getBooleanProperty(String.format("%s.%s", prefix, propertyName));
    }

    private Provider<List<String>> getSpaceSeparatedListLocalProperty(String propertyName) {
        return super.getSpaceSeparatedListProperty(String.format("%s.%s", prefix, propertyName));
    }

    @Override
    protected Provider<String> getStringProperty(String propertyName) {
        return getIsEnabled().zip(
                getStringLocalProperty(propertyName),
                (enabled, value) -> enabled ? value : ""
        );
    }

    @Override
    protected Provider<Boolean> getBooleanProperty(String propertyName) {
        return getIsEnabled().zip(
                getBooleanLocalProperty(propertyName),
                (enabled, value) -> enabled ? value : false
        );
    }

    @Override
    protected Provider<List<String>> getSpaceSeparatedListProperty(String propertyName) {
        return getIsEnabled().zip(
                getSpaceSeparatedListLocalProperty(propertyName),
                (enabled, value) -> enabled ? value : List.of()
        );
    }
}
