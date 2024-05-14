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
                getBooleanProperty("enabled").orElse(true)
        );
    };

    public WithEnabledProperty(WithEnabledProperty parent, String prefix) {
        super(parent.project);

        this.prefix = String.format("%s.%s", parent.prefix, prefix);
        getIsEnabled().convention(
                parent.getIsEnabled().zip(getBooleanProperty("enabled"), (parentEnabled, enabled) -> parentEnabled && enabled).orElse(true)
        );
    }

    public abstract Property<Boolean> getIsEnabled();

    @Override
    protected Provider<String> getStringProperty(String propertyName) {
        return super.getStringProperty(String.format("%s.%s", prefix, propertyName));
    }

    @Override
    protected Provider<Boolean> getBooleanProperty(String propertyName) {
        return super.getBooleanProperty(String.format("%s.%s", prefix, propertyName));
    }

    @Override
    protected Provider<List<String>> getSpaceSeparatedListProperty(String propertyName) {
        return super.getSpaceSeparatedListProperty(String.format("%s.%s", prefix, propertyName));
    }
}
