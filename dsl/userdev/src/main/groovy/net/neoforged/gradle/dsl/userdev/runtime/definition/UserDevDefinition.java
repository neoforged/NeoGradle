package net.neoforged.gradle.dsl.userdev.runtime.definition;

import groovy.transform.CompileStatic;
import net.neoforged.gradle.dsl.common.runtime.definition.LegacyDefinition;
import net.neoforged.gradle.dsl.neoform.runtime.definition.NeoFormDefinition;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.dsl.userdev.runtime.specification.UserDevSpecification;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileTree;

@CompileStatic
public interface UserDevDefinition<S extends UserDevSpecification> extends LegacyDefinition<S> {
    NeoFormDefinition<?> getNeoFormRuntimeDefinition();

    FileTree getUnpackedUserDevJarDirectory();

    UserdevProfile getUserdevConfiguration();

    Configuration getAdditionalUserDevDependencies();
}
