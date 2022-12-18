package net.minecraftforge.gradle.common.extensions;

import groovy.lang.GroovyObjectSupport;
import net.minecraftforge.gradle.common.runtime.naming.NamingChannelProvider;
import net.minecraftforge.gradle.common.util.IConfigurableObject;
import org.gradle.api.Project;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

/**
 * Defines a holder for mapping artifact specification.
 */
public abstract class MappingsExtension extends GroovyObjectSupport implements IConfigurableObject<MappingsExtension> {

    private final Project project;
    private final MinecraftExtension minecraftExtension;

    @Inject
    public MappingsExtension(Project project, MinecraftExtension minecraftExtension) {
        this.project = project;
        this.minecraftExtension = minecraftExtension;
    }

    /**
     * @return The project this extension belongs to.
     */
    public Project getProject() {
        return project;
    }

    /**
     * The mcp minecraft extension this mappings extension belongs to.
     *
     * @return The mcp minecraft extension this mappings extension belongs to.
     */
    public MinecraftExtension getMcpMinecraftExtension() {
        return minecraftExtension;
    }

    /**
     * The channel to pull the mappings from.
     *
     * @return The channel to pull the mappings from.
     */
    @Input
    @Optional
    public abstract Property<NamingChannelProvider> getMappingChannel();

    /**
     * The version to pull the mappings from.
     *
     * @return The version to pull the mappings from.
     */
    @Input
    @Optional
    public abstract MapProperty<String, String> getMappingVersion();

    public Object methodMissing(String name, Object args) {
        if (getMcpMinecraftExtension().getNamingChannelProviders().findByName(name) != null) {
            return getMcpMinecraftExtension().getNamingChannelProviders().named(name);
        }

        return super.invokeMethod(name, args);
    }
}
