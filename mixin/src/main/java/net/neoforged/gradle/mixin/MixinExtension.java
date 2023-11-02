package net.neoforged.gradle.mixin;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.mixin.extension.Mixin;
import org.gradle.api.Project;
import org.gradle.api.provider.SetProperty;

import javax.inject.Inject;

public abstract class MixinExtension implements Mixin, ConfigurableDSLElement<Mixin> {

    private final Project project;
    private final SetProperty<String> configs;

    @Inject
    public MixinExtension(Project project) {
        this.project = project;
        this.configs = project.getObjects().setProperty(String.class);
    }

    @Override
    public Project getProject() {
        return project;
    }
    
    @Override
    public SetProperty<String> getConfigs() {
        return configs;
    }
}
