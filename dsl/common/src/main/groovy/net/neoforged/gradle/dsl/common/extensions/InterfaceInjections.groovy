package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElementWithFilesAndEntries
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Action
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.file.ConfigurableFileCollection

/**
 * Defines a DSL extension which allows for the specification of interface injections.
 */
@CompileStatic
interface InterfaceInjections extends Dependencies {

    /**
     * {@return interface injection files}
     */
    @DSLProperty
    ConfigurableFileCollection getFiles()

    /**
     * {@return interface injections to add as dependencies}
     */
    DependencyCollector getConsume()

    /**
     * {@return interface injections to add as dependencies and also expose to consumers}
     */
    DependencyCollector getConsumeApi()

    /**
     * Publishes a transitive dependency on the given access transformer in the published interface injections of this component.
     *
     * @param dependency to expose to consumers
     */
    void expose(Dependency dependency)

    /**
     * Publishes the provided access transformer as an artifact.
     *
     * @param path access transformer file to publish
     */
    void expose(Object path)

    /**
     * Publishes the provided access transformer as an artifact and configures it with the provided action.
     * @param path access transformer file to publish
     * @param action configures the published artifact
     */
    void expose(Object path, Action<ConfigurablePublishArtifact> action)
}