package net.neoforged.gradle.dsl.common.runs.idea.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

@CompileStatic
abstract class IdeaRunsExtension implements BaseDSLElement<IdeaRunsExtension> {

    IdeaRunsExtension() {
        getRunWithIdea().convention(false);
        getOutDirectory().convention(getProject().getLayout().getProjectDirectory().dir("out"))
    }

    @Inject
    @Override
    abstract Project getProject();

    @DSLProperty
    abstract Property<Boolean> getRunWithIdea();

    @DSLProperty
    abstract DirectoryProperty getOutDirectory();
}