package net.minecraftforge.gradle.common.extensions;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;

public final class SourceSetProjectExtension {
    public static final String NAME = "projectHolder";

    private final Project project;

    @Inject
    public SourceSetProjectExtension(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public static Project get(SourceSet sourceSet) {
        return sourceSet.getExtensions().getByType(SourceSetProjectExtension.class).getProject();
    }
}
