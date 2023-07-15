package net.neoforged.gradle.userdev.extension;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.userdev.extension.UserDev;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class UserDevExtension implements UserDev, ConfigurableDSLElement<UserDev> {

    private final Project project;

    @Inject
    public UserDevExtension(Project project) {
        this.project = project;

        this.getDefaultForgeName().convention("neoforge");
        this.getDefaultForgeGroup().convention("net.neoforged");
    }

    public Project getProject() {
        return project;
    }
}
