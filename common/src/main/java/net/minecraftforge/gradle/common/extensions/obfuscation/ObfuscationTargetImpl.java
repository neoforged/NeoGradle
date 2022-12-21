package net.minecraftforge.gradle.common.extensions.obfuscation;

import net.minecraftforge.gradle.dsl.common.extensions.obfuscation.ObfuscationTarget;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public abstract class ObfuscationTargetImpl implements ObfuscationTarget {

    private final Project project;

    protected ObfuscationTargetImpl(Project project) {
        this.project = project;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public abstract Property<String> getMinecraftVersion();
}
