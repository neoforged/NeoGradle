package net.neoforged.gradle.common.extensions;

import groovy.lang.MissingMethodException;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.util.MappingUtils;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Defines a holder for mapping artifact specification.
 */
public abstract class MappingsExtension implements ConfigurableDSLElement<Mappings>, Mappings {

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

    @Override
    public void version(String version) {
        getVersion().put(NamingConstants.Version.VERSION, version);
    }

    public Object methodMissing(String name, Object args) {
        if (getMinecraft().getNamingChannels().findByName(name) != null) {
            LOGGER.info("Getting mappings from channel provider {}", name);
            return getMinecraft().getNamingChannels().named(name);
        }

        throw new MissingMethodException(name, this.getClass(), new Object[] {args});
    }
}
