package net.neoforged.gradle.dsl.mcp.runtime.definition

import groovy.transform.CompileStatic;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition
import net.neoforged.gradle.dsl.mcp.configuration.McpConfigConfigurationSpecV2
import net.neoforged.gradle.dsl.mcp.runtime.specification.McpSpecification;
import org.jetbrains.annotations.NotNull

/**
 * Represents the definition of a MCP runtime.
 *
 * @param <S> The type of the runtime specification, which is used to configure the runtime.
 */
@CompileStatic
interface McpDefinition<S extends McpSpecification> extends Definition<S> {

    /**
     * The file which defines a directory containing the unpacked MCP runtime.
     *
     * @return The file which defines a directory containing the unpacked MCP runtime.
     */
    @NotNull File getUnpackedMcpZipDirectory();

    /**
     * The deserialized MCP configuration.
     *
     * @return The deserialized MCP configuration.
     */
    @NotNull McpConfigConfigurationSpecV2 getMcpConfig();
}
