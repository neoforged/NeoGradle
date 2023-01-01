package net.minecraftforge.gradle.dsl.userdev.runtime.definition;

import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition;
import net.minecraftforge.gradle.dsl.mcp.runtime.definition.McpDefinition;
import net.minecraftforge.gradle.dsl.userdev.configurations.UserDevConfigurationSpecV2;
import net.minecraftforge.gradle.dsl.userdev.runtime.specification.UserDevSpecification;
import org.gradle.api.artifacts.Configuration;

import java.io.File;

public interface UserDevDefinition<S extends UserDevSpecification> extends Definition<S> {
    McpDefinition<?> getMcpRuntimeDefinition();

    File getUnpackedUserDevJarDirectory();

    UserDevConfigurationSpecV2 getUserdevConfiguration();

    Configuration getAdditionalUserDevDependencies();
}
