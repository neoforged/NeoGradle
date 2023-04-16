package net.minecraftforge.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import net.minecraftforge.gdi.BaseDSLElement
import org.gradle.api.provider.Provider

/**
 * Interface which defines the DSL for an artifact downloader.
 * Classes that implement this interface ad here to the FG DSL for downloading different files and version information through gradle.
 */
@CompileStatic
interface ArtifactDownloader extends BaseDSLElement<ArtifactDownloader> {

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
