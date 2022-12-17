package net.minecraftforge.gradle.common.extensions;

import net.minecraftforge.gradle.common.util.IConfigurableObject;
import net.minecraftforge.gradle.common.util.Utils;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public abstract class DeobfuscationExtension implements IConfigurableObject<DeobfuscationExtension> {

    private final Project project;

    public DeobfuscationExtension(final Project project) {
        this.project = project;

        getForgeFlowerVersion().convention(Utils.FORGEFLOWER_VERSION);
    }

    public Project getProject() {
        return this.project;
    }

    public abstract Property<String> getForgeFlowerVersion();
}
