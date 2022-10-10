package net.minecraftforge.gradle.mcp.extensions;

import net.minecraftforge.gradle.common.util.IConfigurableObject;
import net.minecraftforge.gradle.mcp.naming.NamingChannelProvider;
import org.gradle.api.Project;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;

/**
 * Defines a holder for mapping artifact specification.
 */
public abstract class MappingsExtension implements IConfigurableObject<MappingsExtension> {

    private final Project project;
    private final McpMinecraftExtension mcpMinecraftExtension;

    @Inject
    public MappingsExtension(Project project, McpMinecraftExtension mcpMinecraftExtension) {
        this.project = project;
        this.mcpMinecraftExtension = mcpMinecraftExtension;
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
    public McpMinecraftExtension getMcpMinecraftExtension() {
        return mcpMinecraftExtension;
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

    public void official() {
        getMappingChannel().set(getMcpMinecraftExtension().getNamingChannelProviders().named("official"));
    }
}
