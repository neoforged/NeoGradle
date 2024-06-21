package net.neoforged.gradle.dsl.userdev.runtime.specification;

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.runtime.spec.LegacySpecification;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a runtime specification for a user dev runtime within the confines of a forge-based project.
 */
@CompileStatic
interface UserDevSpecification extends LegacySpecification {

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
    interface Builder<S extends UserDevSpecification, B extends Builder<S, B>> extends LegacySpecification.Builder<S, B> {

        /**
         * Configures the forge group to use.
         * Pulls the value lazily from the given provider.
         *
         * @param forgeGroup The forge group to use.
         * @return The builder.
         */
        B withForgeGroup(Provider<String> forgeGroup);

        /**
         * Configures the forge group to use.
         *
         * @param forgeGroup The forge group to use.
         * @return The builder.
         */
        B withForgeGroup(String forgeGroup);

        /**
         * Configures the forge identifier to use.
         * Pulls the value lazily from the given provider.
         *
         * @param forgeName The forge identifier to use.
         * @return The builder.
         */
        B withForgeName(Provider<String> forgeName);

        /**
         * Configures the forge identifier to use.
         *
         * @param forgeName The forge identifier to use.
         * @return The builder.
         */
        B withForgeName(String forgeName);
        
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
