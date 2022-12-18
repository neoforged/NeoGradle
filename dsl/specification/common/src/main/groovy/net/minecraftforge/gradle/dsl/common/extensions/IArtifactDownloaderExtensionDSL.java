package net.minecraftforge.gradle.dsl.common.extensions;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.gradle.dsl.annotations.NotConfigurableForDSL;
import org.gradle.api.provider.Provider;

import java.io.File;

/**
 * Interface which defines the DSL for an artifact downloader.
 * Classes that implement this interface ad here to the FG DSL for downloading different files and version information through gradle.
 *
 * @apiNote Classes that implement this should never store state in the class itself and as such should never be configurable.
 */
@NotConfigurableForDSL(reason = "Classes which implement this should never store state and as such are not configurable.")
public interface IArtifactDownloaderExtensionDSL {

    /**
     * Creates a provider that looks up the resolved version.
     *
     * @param notation The notation to look up the version for.
     * @return The provider that will resolve the version.
     */
    @NotNull
    Provider<String> version(String notation);

    /**
     * Creates a provider that looks up the resolved file.
     *
     * @param notation The notation to look up the file for.
     * @return The provider that will resolve the file.
     */
    @NotNull
    Provider<File> file(String notation);
}
