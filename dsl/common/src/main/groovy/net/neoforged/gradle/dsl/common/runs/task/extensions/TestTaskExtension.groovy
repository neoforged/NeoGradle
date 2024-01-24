package net.neoforged.gradle.dsl.common.runs.task.extensions

import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet

import javax.inject.Inject

/**
 * An extension used to configure the {@code test} task.
 */
abstract class TestTaskExtension {
    public static final String NAME = 'minecraft'

    /**
     * A list of sources to use as mod classes.
     */
    @Input
    @DSLProperty
    abstract ListProperty<SourceSet> getTestSources()

    @Inject
    TestTaskExtension(Project project) {
        testSources.convention([])
        testSources.add(project.getExtensions().getByType(JavaPluginExtension).sourceSets.named('test'))
    }
}
