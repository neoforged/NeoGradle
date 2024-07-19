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

    private final Project project;
    private final Property<Boolean> runWithIdea;
    private final DirectoryProperty outDirectory;

    @Inject
    IdeaRunsExtension(final Project project) {
        this.project = project;

        this.runWithIdea = project.getObjects().property(Boolean);
        this.outDirectory = project.getObjects().directoryProperty();

        getRunWithIdea().convention(false);
    }

    @Override
    Project getProject() {
        return project;
    }

    @DSLProperty
    Property<Boolean> getRunWithIdea() {
        return runWithIdea;
    }
}