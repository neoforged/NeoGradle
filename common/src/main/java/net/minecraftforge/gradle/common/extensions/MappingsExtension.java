package net.minecraftforge.gradle.common.extensions;

import net.minecraftforge.gradle.base.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Defines a holder for mapping artifact specification.
 */
public abstract class MappingsExtension extends ConfigurableObject<Mappings> implements Mappings {

    private static final Logger LOGGER = LoggerFactory.getLogger(MappingsExtension.class);

    private final Project project;
    private final Minecraft minecraftExtension;

    @Inject
    public MappingsExtension(Project project) {
        this.project = project;
        this.minecraftExtension = project.getExtensions().getByType(Minecraft.class);
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Minecraft getMinecraft() {
        return minecraftExtension;
    }

    public Object methodMissing(String name, Object args) {
        if (getMinecraft().getNamingChannelProviders().findByName(name) != null) {
            LOGGER.info("Getting mappings from channel provider {}", name);
            return getMinecraft().getNamingChannelProviders().named(name);
        }

        return super.invokeMethod(name, args);
    }
}
