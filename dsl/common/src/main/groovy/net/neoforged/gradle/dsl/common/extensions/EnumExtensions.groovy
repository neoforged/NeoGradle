package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.neoforged.gdi.BaseDSLElement
import net.neoforged.gdi.annotations.DSLProperty
import org.gradle.api.Action
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.file.ConfigurableFileCollection

/**
 * Defines a DSL extension which allows for the specification of enum extensions.
 */
@CompileStatic
interface EnumExtensions extends BaseDSLElement<EnumExtensions>, Dependencies {

    /**
     * {@return enum extensions files}
     */
    @DSLProperty
    ConfigurableFileCollection getFiles()

    /**
     * {@return enum extensions to add as dependencies}
     */
    DependencyCollector getConsume()

    /**
     * {@return enum extensions to add as dependencies and also expose to consumers}
     */
    DependencyCollector getConsumeApi()

    /**
     * Publishes a transitive dependency on the given interface injection in the published enum extensions of this component.
     *
     * @param dependency to expose to consumers
     */
    @Deprecated(forRemoval = true, since="7.1")
    void expose(Dependency dependency)

    /**
     * Publishes a transitive dependency on the given interface injection in the published enum extensions of this component.
     *
     * @param path access transformer file to publish
     */
    void expose(Object path)

    /**
     * Publishes a given interface injection in the published enum extensions of this component.
     *
     * @param path enum extensions file to publish
     * @param action configures the published artifact
     */
    void expose(Object path, Action<ConfigurablePublishArtifact> action)
}