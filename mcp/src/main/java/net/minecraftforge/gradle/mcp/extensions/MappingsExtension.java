package net.minecraftforge.gradle.mcp.extensions;

import groovy.cli.Option;
import groovy.lang.Closure;
import net.minecraftforge.gradle.common.util.IConfigurableObject;
import net.minecraftforge.gradle.mcp.naming.NamingChannelProvider;
import net.minecraftforge.gradle.mcp.runtime.tasks.McpRuntime;
import net.minecraftforge.gradle.mcp.util.MappingUtils;
import org.gradle.api.Project;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.inject.Inject;
import java.util.Map;

/**
 * Defines a holder for mapping artifact specification.
 */
public abstract class MappingsExtension implements IConfigurableObject<MappingsExtension> {

    private final Project project;

    @Inject
    public MappingsExtension(Project project) {
        this.project = project;
    }

    /**
     * @return The project this extension belongs to.
     */
    public Project getProject() {
        return project;
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
}
