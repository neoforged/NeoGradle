package net.neoforged.gradle.common.extensions.subsystems;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class SubsystemsExtension implements ConfigurableDSLElement<Subsystems>, Subsystems {
    private final Project project;

    @Inject
    public SubsystemsExtension(Project project) {
        this.project = project;
    }

    @Override
    public Project getProject() {
        return project;
    }
}
