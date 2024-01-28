package net.neoforged.gradle.dsl.common.runs.task.extensions

import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet

import javax.inject.Inject

/**
 * An extension used to configure the {@code test} task.
 */
class TestTaskExtension {
    public static final String NAME = 'minecraft'

    private final ListProperty<SourceSet> testSources
    private final Property<Boolean> minecraftEnvironment

    /**
     * A list of sources to use as mod classes.
     */
    @Input
    @DSLProperty
    ListProperty<SourceSet> getTestSources() {
        return testSources
    }

    /**
     * If {@code false}, the test task won't be configured to run a Minecraft environment.
     */
    @Input
    @DSLProperty
    Property<Boolean> isMinecraftEnvironment() {
        return minecraftEnvironment
    }

    @Inject
    TestTaskExtension(Project project, String name) {
        testSources = project.objects.listProperty(SourceSet)
        testSources.convention([])
        testSources.add(project.getExtensions().getByType(JavaPluginExtension).sourceSets.named('main'))
        testSources.add(project.getExtensions().getByType(JavaPluginExtension).sourceSets.named('test'))

        minecraftEnvironment = project.objects.property(Boolean)
        minecraftEnvironment.convention(name == JavaPlugin.TEST_TASK_NAME)
    }
}
