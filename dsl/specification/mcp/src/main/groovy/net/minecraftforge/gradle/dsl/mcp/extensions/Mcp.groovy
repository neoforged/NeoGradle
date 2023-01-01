package net.minecraftforge.gradle.dsl.mcp.extensions

import groovy.transform.CompileStatic;
import net.minecraftforge.gradle.dsl.annotations.DSLProperty;
import net.minecraftforge.gradle.dsl.base.BaseDSLElement;
import net.minecraftforge.gradle.dsl.common.util.Artifact;
import org.gradle.api.provider.Property;

/**
 * Defines the configuration of the MCP subsystem
 */
@CompileStatic
interface Mcp extends BaseDSLElement<Mcp> {

    /**
     * Configures the default MCP Config version to use.
     * Not required if the MCP Config artifact is specified, or the MCP Config version is specified in the mcp config dependency directly.
     *
     * @return The default mcp config version.
     */
    @DSLProperty
    Property<String> getMcpConfigVersion();

    /**
     * Configures the default MCP Config artifact to use.
     * Not required if the MCP Config version is specified, or the MCP Config version is specified in the mcp config dependency directly.
     *
     * @return The default mcp config artifact.
     */
    @DSLProperty
    Property<Artifact> getMcpConfigArtifact();
}
