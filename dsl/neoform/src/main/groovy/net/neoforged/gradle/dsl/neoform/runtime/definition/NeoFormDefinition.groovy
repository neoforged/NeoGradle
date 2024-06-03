package net.neoforged.gradle.dsl.neoform.runtime.definition

import groovy.transform.CompileStatic;
import net.neoforged.gradle.dsl.common.runtime.definition.LegacyDefinition
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV2
import net.neoforged.gradle.dsl.neoform.runtime.specification.NeoFormSpecification;
import org.jetbrains.annotations.NotNull

/**
 * Represents the definition of a NeoForm runtime.
 *
 * @param <S> The type of the runtime specification, which is used to configure the runtime.
 */
@CompileStatic
interface NeoFormDefinition<S extends NeoFormSpecification> extends LegacyDefinition<S> {

    /**
     * The deserialized NeoForm configuration.
     *
     * @return The deserialized NeoForm configuration.
     */
    @NotNull NeoFormConfigConfigurationSpecV2 getNeoFormConfig();
}
