package net.minecraftforge.gradle.userdev.extension;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.minecraftforge.gradle.dsl.userdev.extension.UserDev;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class UserDevExtension implements UserDev, ConfigurableDSLElement<UserDev> {

    private final Project project;

    @Inject
    public UserDevExtension(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }
}
