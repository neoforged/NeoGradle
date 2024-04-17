package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElementWithFilesAndEntries
import org.gradle.api.Action
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.dsl.DependencyCollector

/**
 * Defines a DSL extension which allows for the specification of access transformers.
 */
@CompileStatic
interface AccessTransformers extends BaseDSLElementWithFilesAndEntries<AccessTransformers> {
    void consume(Object notation)

    void consumeApi(Object notation)

    void expose(Object path)

    void expose(Object path, Action<ConfigurablePublishArtifact> action)
}