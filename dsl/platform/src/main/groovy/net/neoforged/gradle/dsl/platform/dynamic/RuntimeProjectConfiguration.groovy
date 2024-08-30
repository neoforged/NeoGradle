package net.neoforged.gradle.dsl.platform.dynamic

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional

import javax.inject.Inject

@CompileStatic
abstract class RuntimeProjectConfiguration implements BaseDSLElement<RuntimeProjectConfiguration> {

    private final Project project

    @Inject
    RuntimeProjectConfiguration(Project project) {
        this.project = project
    }

    Project getProject() {
        return project
    }

    @DSLProperty
    abstract Property<String> getNeoFormVersion()

    /**
     * @return The directory containing the patches to apply to the project.
     */
    @DSLProperty
    abstract DirectoryProperty getPatches()

    /**
     * @return The directory containing the rejects from applying patches to the project.
     */
    @DSLProperty
    abstract DirectoryProperty getRejects()

    /**
     * @return The directory containing the legacy patches to migrate when split source sets are enabled.
     */
    @Optional
    @DSLProperty
    abstract DirectoryProperty getLegacyPatches()

    /**
     * @return True if the source sets should be split based on distribution, false otherwise.
     */
    @DSLProperty
    abstract Property<Boolean> getSplitSourceSets();
}
