package net.neoforged.gradle.common.extensions.dependency.replacement;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacer;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public abstract class DependencyReplacementHandlerImpl implements ConfigurableDSLElement<DependencyReplacementHandler>, DependencyReplacementHandler {

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
    public abstract Property<DependencyReplacer> getReplacer();
}