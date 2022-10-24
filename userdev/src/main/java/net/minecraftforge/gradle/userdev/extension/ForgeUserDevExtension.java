package net.minecraftforge.gradle.userdev.extension;

import groovy.lang.GroovyObjectSupport;
import net.minecraftforge.gradle.common.util.IConfigurableObject;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class ForgeUserDevExtension extends GroovyObjectSupport implements IConfigurableObject<ForgeUserDevExtension> {

    private final Project project;

    @Inject
    public ForgeUserDevExtension(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public abstract Property<String> getDefaultVersion();
}
