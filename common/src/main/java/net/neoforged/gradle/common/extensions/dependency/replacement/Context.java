package net.neoforged.gradle.common.extensions.dependency.replacement;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Context implements net.neoforged.gradle.dsl.common.extensions.dependency.replacement.Context {
    private final @NotNull Project project;
    private final @NotNull Configuration configuration;
    private final @NotNull ModuleDependency dependency;
    private final @Nullable Context parent;

    public Context(
            @NotNull Project project,
            @NotNull Configuration configuration,
            @NotNull ModuleDependency dependency,
            @Nullable Context parent) {
        this.project = project;
        this.configuration = configuration;
        this.dependency = dependency;
        this.parent = parent;
    }

    @Override
    public @NotNull Project getProject() {
        return project;
    }

    @Override
    public @NotNull Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public @NotNull ModuleDependency getDependency() {
        return dependency;
    }

    @Nullable
    @Override
    public Context getParent() {
        return parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Context)) return false;

        Context that = (Context) o;

        if (!getProject().equals(that.getProject())) return false;
        if (!getConfiguration().equals(that.getConfiguration())) return false;
        if (!getDependency().equals(that.getDependency())) return false;
        return getParent() != null ? getParent().equals(that.getParent()) : that.getParent() == null;
    }

    @Override
    public int hashCode() {
        int result = getProject().hashCode();
        result = 31 * result + getConfiguration().hashCode();
        result = 31 * result + getDependency().hashCode();
        result = 31 * result + (getParent() != null ? getParent().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DependencyReplacementContext{" +
                "project=" + project +
                ", configuration=" + configuration +
                ", dependency=" + dependency +
                ", parent=" + parent +
                '}';
    }
}
