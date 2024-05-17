package net.neoforged.gradle.common.extensions.repository;

import net.minecraftforge.gdi.BaseDSLElement;
import net.neoforged.gradle.dsl.common.extensions.repository.Entry;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.util.ModuleDependencyUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;

import javax.inject.Inject;
import java.io.Serializable;

public abstract class IvyEntry implements BaseDSLElement<Entry>, Entry, Serializable {

    private final Project project;
    private final Dependency original;
    private final Dependency dependency;
    private final Configuration dependencies;
    private final boolean hasSources;

    @Inject
    public IvyEntry(Project project, Dependency original, Dependency dependency, Configuration dependencies, boolean hasSources) {
        this.project = project;
        this.original = original;
        this.dependency = dependency;
        this.dependencies = dependencies;
        this.hasSources = hasSources;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Dependency getOriginal() {
        return original;
    }

    @Override
    public Dependency getDependency() {
        return dependency;
    }

    @Override
    public Configuration getDependencies() {
        return dependencies;
    }

    @Override
    public boolean hasSources() {
        return hasSources;
    }

    public abstract static class Builder implements Entry.Builder {

        private final Project project;

        private Dependency original;
        private Dependency dependency;
        private Configuration dependencies;
        private boolean hasSources = true;

        @Inject
        public Builder(Project project) {
            this.project = project;
        }

        @Override
        public Project getProject() {
            return project;
        }

        @Override
        public Entry.Builder from(Dependency dependency) {
            return from(
                    dependency,
                    ConfigurationUtils.temporaryUnhandledConfiguration(
                            project.getConfigurations(),
                            "EmptyDependenciesFor" + ModuleDependencyUtils.toConfigurationName(dependency)
                    )
            );
        }

        @Override
        public Entry.Builder from(Dependency dependency, Configuration dependencies) {
            this.original = dependency;
            this.dependency = wrap(dependency);
            this.dependencies = dependencies;
            return this;
        }

        @Override
        public Entry.Builder withoutSources() {
            this.hasSources = false;
            return this;
        }

        @Override
        public Entry build() {
            return project.getObjects().newInstance(IvyEntry.class, project, original, dependency, dependencies, hasSources);
        }

        private Dependency wrap(Dependency dependency) {
            if (!(dependency instanceof ExternalModuleDependency))
                throw new IllegalArgumentException("Dependency must be an ExternalModuleDependency");

            final String gav = ModuleDependencyUtils.format((ExternalModuleDependency) dependency);

            return project.getDependencies().create("ng_dummy_ng." + gav);
        }
    }
}
