package net.neoforged.gradle.common.util.run;

import org.gradle.api.attributes.Attribute;

/**
 * Attributes for variant-aware mod source configurations.
 * 
 * Used to carry metadata about mod sources through Gradle's variant resolution system,
 * enabling isolated projects-compatible cross-project mod source declarations via dependencies.
 */
public final class ModSourceAttributes {
    
    private ModSourceAttributes() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Attribute key identifying the project path of a mod source (e.g., ":api" or ":libs:core").
     */
    public static final Attribute<String> MOD_SOURCE_PROJECT_PATH = 
        Attribute.of("net.neoforged.gradle.mod-source-project-path", String.class);

    /**
     * Attribute key identifying the source set name of a mod source (e.g., "main" or "test").
     */
    public static final Attribute<String> MOD_SOURCE_NAME = 
        Attribute.of("net.neoforged.gradle.mod-source-name", String.class);

    /**
     * Marker attribute that distinguishes modSource variants from other NeoGradle variants.
     * This prevents Gradle's "identical capabilities" error when multiple consumable configurations
     * exist within the same project with overlapping attributes.
     */
    public static final Attribute<String> MOD_SOURCE_TYPE = 
        Attribute.of("net.neoforged.gradle.mod-source-type", String.class);

    /**
     * Value for MOD_SOURCE_TYPE indicating this is a mod source variant.
     */
    public static final String TYPE_MOD_SOURCE = "mod-source";
}
