package net.minecraftforge.gradle.common.extensions.dependency.replacement;

import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacer;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public abstract class DependencyReplacementHandlerImpl extends ConfigurableObject<DependencyReplacementHandler> implements DependencyReplacementHandler {

    private final Project project;
    private final String name;

    @Inject
    public DependencyReplacementHandlerImpl(Project project, String name) {
        this.project = project;
        this.name = name;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    @Inject
    public abstract Property<DependencyReplacer> getReplacer();
}