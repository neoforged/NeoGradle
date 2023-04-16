package net.minecraftforge.gradle.dsl.userdev.runtime.specification;

import groovy.transform.CompileStatic;
import net.minecraftforge.gradle.dsl.common.runtime.spec.Specification;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a runtime specification for a user dev runtime within the confines of a forge-based project.
 */
@CompileStatic
interface UserDevSpecification extends Specification {

    /**
     * Gets the version of forge to use.
     *
     * @return The version of forge to use.
     */
    @NotNull
    String getForgeVersion();

    /**
     * Defines the builder for a userdev specification.
     *
     * @param <S> The type of specification to build.
     * @param <B> The self-type of the builder
     */
    @CompileStatic
    interface Builder<S extends UserDevSpecification, B extends Builder<S, B>> extends Specification.Builder<S, B> {

        /**
         * Configures the forge version to use.
         * Pulls the value lazily from the given provider.
         *
         * @param forgeVersion The forge version to use.
         * @return The builder.
         */
        B withForgeVersion(Provider<String> forgeVersion);

        /**
         * Configures the forge version to use.
         *
         * @param forgeVersion The forge version to use.
         * @return The builder.
         */
        B withForgeVersion(String forgeVersion);
    }
}
