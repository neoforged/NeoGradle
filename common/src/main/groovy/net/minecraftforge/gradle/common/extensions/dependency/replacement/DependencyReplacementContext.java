package net.minecraftforge.gradle.common.extensions.dependency.replacement;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class DependencyReplacementContext {
        private final @NotNull Project project;
        private final @NotNull Configuration configuration;
        private final @NotNull ModuleDependency dependency;

        public DependencyReplacementContext(
                @NotNull Project project,
                @NotNull Configuration configuration,
                @NotNull ModuleDependency dependency
        ) {
                this.project = project;
                this.configuration = configuration;
                this.dependency = dependency;
        }

        public @NotNull Project project() {
                return project;
        }

        public @NotNull Configuration configuration() {
                return configuration;
        }

        public @NotNull ModuleDependency dependency() {
                return dependency;
        }

        @Override
        public boolean equals(Object obj) {
                if (obj == this) return true;
                if (obj == null || obj.getClass() != this.getClass()) return false;
                final DependencyReplacementContext that = (DependencyReplacementContext) obj;
                return Objects.equals(this.project, that.project) &&
                        Objects.equals(this.configuration, that.configuration) &&
                        Objects.equals(this.dependency, that.dependency);
        }

        @Override
        public int hashCode() {
                return Objects.hash(project, configuration, dependency);
        }

        @Override
        public String toString() {
                return "DependencyReplacementContext[" +
                        "project=" + project + ", " +
                        "configuration=" + configuration + ", " +
                        "dependency=" + dependency + ']';
        }

}
