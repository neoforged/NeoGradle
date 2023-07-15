package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.dsl.common.extensions.ProjectHolder;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class ProjectHolderExtension implements ProjectHolder {
    public static final String NAME = "projectHolder";
    private final Project project;

    @Inject
    public ProjectHolderExtension(Project project) {
        this.project = project;
    }

    @Override
    public Project getProject() {
        return project;
    }
}
