package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.tasks.JarJar;
import net.neoforged.gradle.dsl.common.dependency.DependencyFilter;
import net.neoforged.gradle.dsl.common.dependency.DependencyVersionInformationHandler;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.publish.maven.MavenPublication;

import javax.inject.Inject;

public class JarJarExtension implements net.neoforged.gradle.dsl.common.extensions.JarJar {
    public static final Attribute<String> JAR_JAR_RANGE_ATTRIBUTE = Attribute.of("jarJarRange", String.class);

    private final Project project;
    private boolean disabled;
    private boolean disableDefaultSources;

    @Inject
    public JarJarExtension(final Project project) {
        this.project = project;
    }

    @Override
    public void enable() {
        if (!this.disabled)
            enable(true);
    }

    private void enable(boolean enabled) {
        final Task task = project.getTasks().findByPath("jarJar");
        if (task != null) {
            task.setEnabled(enabled);
        }
    }

    @Override
    public void disable() {
        disable(true);
    }

    @Override
    public void disable(boolean disable) {
        this.disabled = disable;
        if (disable) {
            enable(false);
        }
    }

    @Override
    public boolean getDefaultSourcesDisabled() {
        return this.disableDefaultSources;
    }

    @Override
    public void disableDefaultSources() {
        disableDefaultSources(true);
    }

    @Override
    public void disableDefaultSources(boolean value) {
        this.disableDefaultSources = value;
    }

    @Override
    public void fromRuntimeConfiguration() {
        enable();
        project.getTasks().withType(JarJar.class).configureEach(JarJar::fromRuntimeConfiguration);
    }

    @Override
    public void pin(Dependency dependency, String version) {
        enable();
        if (dependency instanceof ExternalDependency) {
            ((ExternalDependency) dependency).version(constraint -> {
                constraint.prefer(version);
            });
        }
    }

    @Override
    public void ranged(Dependency dependency, String range) {
        enable();
        if (dependency instanceof ModuleDependency) {
            final ModuleDependency moduleDependency = (ModuleDependency) dependency;
            moduleDependency.attributes(attributeContainer -> attributeContainer.attribute(JAR_JAR_RANGE_ATTRIBUTE, range));
        }
    }

    @Override
    public JarJarExtension dependencies(Action<DependencyFilter> c) {
        enable();
        project.getTasks().withType(JarJar.class).configureEach(jarJar -> jarJar.dependencies(c));
        return this;
    }

    @Override
    public JarJarExtension versionInformation(Action<DependencyVersionInformationHandler> c) {
        enable();
        project.getTasks().withType(JarJar.class).configureEach(jarJar -> jarJar.versionInformation(c));
        return this;
    }

    @Override
    public MavenPublication component(MavenPublication mavenPublication) {
        return component(mavenPublication, true);
    }

    public MavenPublication component(MavenPublication mavenPublication, boolean handleDependencies) {
        enable();
        project.getTasks().withType(JarJar.class).configureEach(task -> component(mavenPublication, task, false, handleDependencies));

        return mavenPublication;
    }

    public MavenPublication component(MavenPublication mavenPublication, JarJar task) {
        enable();
        return component(mavenPublication, task, true, true);
    }

    public MavenPublication cleanedComponent(MavenPublication mavenPublication, JarJar task, boolean handleDependencies) {
        enable();
        return component(mavenPublication, task, true, handleDependencies);
    }

    private MavenPublication component(MavenPublication mavenPublication, JarJar task, boolean handleCleaning) {
        return component(mavenPublication, task, handleCleaning, true);
    }

    private MavenPublication component(MavenPublication mavenPublication, JarJar task, boolean handleCleaning, boolean handleDependencies) {
        if (!task.isEnabled()) {
            return mavenPublication;
        }

        if (handleCleaning) {
            //TODO: Handle this gracefully somehow?
        }

        mavenPublication.artifact(task, mavenArtifact -> {
            mavenArtifact.setClassifier(task.getArchiveClassifier().get());
            mavenArtifact.setExtension(task.getArchiveExtension().get());
        });

        if (handleDependencies) {
            //TODO: Handle this gracefully.
        }

        return mavenPublication;
    }
}
