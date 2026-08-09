package net.neoforged.gradle.common.util.run;

import org.gradle.api.attributes.Attribute;

/**
 * Attributes for variant-aware run preparation configurations.
 * 
 * Used to distinguish between compile-required and resources-only variants of source set outputs,
 * enabling isolated projects-compatible task dependency wiring for run tasks.
 */
public final class RunPreparationAttributes {
    
    private RunPreparationAttributes() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Attribute key distinguishing run preparation variants.
     * 
     * Values:
     * - {@link #MODE_COMPILE}: Requires full compilation (Gradle-launched runs)
     * - {@link #MODE_RESOURCES_ONLY}: Only processes resources, no compile (IDE runs)
     */
    public static final Attribute<String> RUN_PREPARATION_MODE = 
        Attribute.of("net.neoforged.gradle.run-preparation-mode", String.class);

    // Values
    public static final String MODE_COMPILE = "compile";
    public static final String MODE_RESOURCES_ONLY = "resources-only";
}
