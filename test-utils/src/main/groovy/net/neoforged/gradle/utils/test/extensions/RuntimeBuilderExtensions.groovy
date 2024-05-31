package net.neoforged.gradle.utils.test.extensions

import groovy.transform.CompileStatic
import net.neoforged.gradle.common.caching.CentralCacheService
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime

/**
 * Extensions for the {@link Runtime.Builder} class.
 */
@CompileStatic
class RuntimeBuilderExtensions {

    /**
     * Sets the global cache directory to the given {@param testProjectDir}.
     *
     * @param testProjectDir the directory to use as the global cache directory
     * @return the global cache directory
     */
    static File withGlobalCacheDirectory(final Runtime.Builder self, final File testProjectDir) {
        final File cacheDir = new File(testProjectDir, ".ng-cache")
        self.property(CentralCacheService.CACHE_DIRECTORY_PROPERTY, cacheDir.propertiesPath)

        return cacheDir
    }
}
