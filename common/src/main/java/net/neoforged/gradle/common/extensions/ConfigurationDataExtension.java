package net.neoforged.gradle.common.extensions;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.ConfigurationData;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public abstract class ConfigurationDataExtension implements ConfigurableDSLElement<ConfigurationData>, ConfigurationData {

    private final Project project;

    public ConfigurationDataExtension(Project project) {
        this.project = project;
        getLocation().convention(getProject().getLayout().getProjectDirectory().dir(".gradle").dir("configuration"));
    }

    @Override
    public Project getProject() {
        return project;
    }
}
