package net.neoforged.gradle.common.runtime.definition;

import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.spec.Specification;

/**
 * Internal type definition for a runtime definition that delegates to another definition.
 *
 * @param <S> The public specification type.
 */
public interface IDelegatingRuntimeDefinition<S extends Specification> extends Definition<S> {

    /**
     * The delegate definition.
     *
     * @return The delegate definition.
     */
    Definition<?> getDelegate();
}
