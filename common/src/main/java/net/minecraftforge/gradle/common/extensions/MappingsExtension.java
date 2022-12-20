package net.minecraftforge.gradle.common.extensions;

import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import org.gradle.api.Project;

import javax.inject.Inject;

/**
 * Defines a holder for mapping artifact specification.
 */
public abstract class MappingsExtension extends ConfigurableObject<Mappings> implements Mappings {

    private final Project project;
    private final MinecraftExtension minecraftExtension;

    @Inject
    public MappingsExtension(Project project) {
        this.project = project;
        this.minecraftExtension = project.getExtensions().getByType(MinecraftExtension.class);
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public MinecraftExtension getMinecraft() {
        return minecraftExtension;
    }

    public Object methodMissing(String name, Object args) {
        if (getMinecraft().getNamingChannelProviders().findByName(name) != null) {
            return getMinecraft().getNamingChannelProviders().named(name);
        }

        return super.invokeMethod(name, args);
    }
}
