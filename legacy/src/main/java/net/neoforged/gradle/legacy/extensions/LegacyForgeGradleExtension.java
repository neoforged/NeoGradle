package net.neoforged.gradle.legacy.extensions;

import groovy.lang.Closure;
import net.neoforged.gradle.common.extensions.ForcedDependencyDeobfuscationExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

@Deprecated
public abstract class LegacyForgeGradleExtension {

    private final Project project;

    @Inject
    public LegacyForgeGradleExtension(Project project) {
        this.project = project;
    }

    @Deprecated
    public Dependency deobf(Object dependency) {
        return deobf(dependency, null);
    }

    @Deprecated
    public Dependency deobf(Object dependency, @Nullable Closure<?> configure) {
        return project.getDependencies().create(dependency, configure);
    }

}
