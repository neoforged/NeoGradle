package net.minecraftforge.gradle.common.extensions;

import net.minecraftforge.gradle.base.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.common.extensions.Deobfuscation;
import net.minecraftforge.gradle.dsl.common.util.Constants;
import org.gradle.api.Project;

public abstract class DeobfuscationExtension extends ConfigurableObject<Deobfuscation> implements Deobfuscation {

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
