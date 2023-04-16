package net.minecraftforge.gradle.common.extensions.obfuscation;

import net.minecraftforge.gradle.dsl.common.extensions.obfuscation.ObfuscationTarget;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public abstract class ObfuscationTargetImpl implements ObfuscationTarget {

    private final Project project;
    private final String name;

    protected ObfuscationTargetImpl(Project project, String name) {
        this.project = project;
        this.name = name;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public abstract Property<String> getMinecraftVersion();

    @Override
    public abstract Property<DistributionType> getDistributionType();
}
