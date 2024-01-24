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
class TestTaskExtension {
    public static final String NAME = 'minecraft'

    private final ListProperty<SourceSet> testSources

    /**
     * A list of sources to use as mod classes.
     */
    @Input
    @DSLProperty
    ListProperty<SourceSet> getTestSources() {
        return testSources
    }

    @Inject
    TestTaskExtension(Project project) {
        testSources = project.objects.listProperty(SourceSet)
        testSources.convention([])
        testSources.add(project.getExtensions().getByType(JavaPluginExtension).sourceSets.named('main'))
        testSources.add(project.getExtensions().getByType(JavaPluginExtension).sourceSets.named('test'))
    }
}
