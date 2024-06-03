package net.neoforged.gradle.common.runtime.definition;

import net.neoforged.gradle.dsl.common.runtime.definition.LegacyDefinition;
import net.neoforged.gradle.dsl.common.runtime.spec.LegacySpecification;

/**
 * Internal type definition for a runtime definition that delegates to another definition.
 *
 * @param <S> The public specification type.
 */
public interface IDelegatingRuntimeDefinition<S extends LegacySpecification> extends LegacyDefinition<S> {

    /**
     * The delegate definition.
     *
     * @return The delegate definition.
     */
    LegacyDefinition<?> getDelegate();
}
