package net.minecraftforge.gradle.dsl.vanilla.runtime.spec;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.gradle.dsl.common.runtime.spec.Specification;

/**
 * Defines a specification for a vanilla runtime.
 */
public interface VanillaSpecification extends Specification {

    /**
     * Gets the version of FART to use.
     *
     * @return The version of FART to use.
     */
    @NotNull
    String getFartVersion();

    /**
     * Gets the version of ForgeFlower to use.
     *
     * @return The version of ForgeFlower to use.
     */
    @NotNull
    String getForgeFlowerVersion();

    /**
     * Gets the version of AccessTransformerApplier to use.
     *
     * @return The version of AccessTransformerApplier to use.
     */
    @NotNull
    String getAccessTransformerApplierVersion();
}
