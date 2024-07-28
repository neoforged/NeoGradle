package net.neoforged.gradle.utils.test.extensions

import net.neoforged.gradle.common.services.caching.CachedExecutionService
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime

/**
 * Extensions for the {@link Runtime.Builder} class.
 */
class RuntimeBuilderExtensions {

    /**
     * Sets the global cache directory to the given {@param testProjectDir}.
     *
     * @param testProjectDir the directory to use as the global cache directory
     * @return the global cache directory
     */
    static File withGlobalCacheDirectory(final Runtime.Builder self, final File testProjectDir) {
        final File cacheDir = new File(testProjectDir, ".ng-cache")
        self.property(CachedExecutionService.CACHE_DIRECTORY_PROPERTY, cacheDir.getPropertiesPath())

        return cacheDir
    }

    /**
     * Adds a manifest file to the runtime.
     *
     * @param self the runtime builder
     * @param attributes the attributes to add to the manifest
     * @return the runtime builder
     */
    static Runtime.Builder withManifest(final Runtime.Builder self, final Map<String, String> attributes) {
        final String content = attributes.collect { k, v -> "$k: $v" }.join(System.lineSeparator())
        self.file("src/main/resources/META-INF/MANIFEST.MF", content)
    }

    /**
     * Disables conventions for the runtime.
     *
     * @param self the runtime builder
     * @return the runtime builder
     */
    static Runtime.Builder disableConventions(final Runtime.Builder self) {
        self.property("neogradle.subsystems.conventions.enabled", "false")
    }

    /**
     * Enables parallel running for the runtime.
     *
     * @param self the runtime builder
     * @return the runtime builder
     */
    static Runtime.Builder enableGradleParallelRunning(final Runtime.Builder self) {
        self.property("org.gradle.parallel", "true")
    }
}
