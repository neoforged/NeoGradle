package net.minecraftforge.gradle.common.extensions;

import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.dsl.common.extensions.ArtifactDownloader;
import net.minecraftforge.gradle.dsl.common.extensions.Deobfuscation;
import org.gradle.api.Project;

public abstract class DeobfuscationExtension extends ConfigurableObject<Deobfuscation> implements Deobfuscation {

    private final Project project;

    public DeobfuscationExtension(final Project project) {
        this.project = project;
        getForgeFlowerVersion().convention(Utils.FORGEFLOWER_VERSION);
    }

    @Override
    public Project getProject() {
        return this.project;
    }
}
