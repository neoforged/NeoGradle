package net.neoforged.gradle.mixin;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.mixin.extension.Mixin;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class MixinExtension implements Mixin, ConfigurableDSLElement<Mixin> {

    private final Project project;

    @Inject
    public MixinExtension(Project project) {
        this.project = project;
        getDisableTargetValidator().convention(false);
        getDisableTargetExport().convention(false);
        getDisableOverwriteChecker().convention(false);
        getQuiet().convention(false);
        getShowMessageTypes().convention(false);
    }

    @Override
    public Project getProject() {
        return project;
    }
}
