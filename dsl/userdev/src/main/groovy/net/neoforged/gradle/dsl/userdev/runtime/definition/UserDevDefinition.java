package net.neoforged.gradle.dsl.userdev.runtime.definition;

import groovy.transform.CompileStatic;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.neoform.runtime.definition.NeoFormDefinition;
import net.neoforged.gradle.dsl.userdev.configurations.UserDevConfigurationSpecV2;
import net.neoforged.gradle.dsl.userdev.runtime.specification.UserDevSpecification;
import org.gradle.api.artifacts.Configuration;

import java.io.File;

@CompileStatic
public interface UserDevDefinition<S extends UserDevSpecification> extends Definition<S> {
    NeoFormDefinition<?> getNeoFormRuntimeDefinition();

    File getUnpackedUserDevJarDirectory();

    @SuppressWarnings("deprecation")
    UserDevConfigurationSpecV2 getUserdevConfiguration();

    Configuration getAdditionalUserDevDependencies();
}
