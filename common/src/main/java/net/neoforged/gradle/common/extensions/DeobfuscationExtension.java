package net.neoforged.gradle.common.extensions;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.Deobfuscation;
import net.neoforged.gradle.dsl.common.util.Constants;
import org.gradle.api.Project;

public abstract class DeobfuscationExtension implements ConfigurableDSLElement<Deobfuscation>, Deobfuscation {

    private final Project project;

    public DeobfuscationExtension(final Project project) {
        this.project = project;
        getForgeFlowerVersion().convention(Constants.FORGEFLOWER_VERSION);
    }

    @Override
    public Project getProject() {
        return this.project;
    }
}
