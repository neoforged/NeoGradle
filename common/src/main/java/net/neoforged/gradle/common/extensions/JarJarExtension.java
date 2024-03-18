package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.tasks.JarJar;
import net.neoforged.gradle.dsl.common.dependency.DependencyFilter;
import net.neoforged.gradle.dsl.common.dependency.DependencyVersionInformationHandler;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.plugins.JavaPlugin;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class JarJarExtension implements net.neoforged.gradle.dsl.common.extensions.JarJar {
    public static final Attribute<String> JAR_JAR_RANGE_ATTRIBUTE = Attribute.of("jarJarRange", String.class);
    public static final Attribute<String> FIXED_JAR_JAR_VERSION_ATTRIBUTE = Attribute.of("fixedJarJarVersion", String.class);

    private final Project project;
    private boolean disabled;
    private boolean enabled;
    private boolean disableDefaultSources;
    private PublishArtifact addedToPublication;
    private final List<PublishArtifact> removedFromPublication = new ArrayList<>();

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
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        final JarJar task = (JarJar) project.getTasks().findByPath("jarJar");
        Configuration runtimeElements = project.getConfigurations().maybeCreate(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME);
        if (task != null) {
            if (enabled) {
                removedFromPublication.clear();
                removedFromPublication.addAll(runtimeElements.getArtifacts());
                runtimeElements.getArtifacts().clear();
                project.artifacts(handler ->
                        addedToPublication = handler.add(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME, task, artifact ->
                                artifact.builtBy(task)
                        )
                );
            } else {
                runtimeElements.getArtifacts().remove(addedToPublication);
                runtimeElements.getArtifacts().addAll(removedFromPublication);
            }
            if (!task.getEnabled() == enabled) {
                task.setEnabled(enabled);
            }
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
    @Deprecated
    public void pin(Dependency dependency, String version) {
        enable();
        if (dependency instanceof ModuleDependency) {
            final ModuleDependency moduleDependency = (ModuleDependency) dependency;
            moduleDependency.attributes(attributeContainer -> attributeContainer.attribute(FIXED_JAR_JAR_VERSION_ATTRIBUTE, version));
        }
    }

    @Override
    @Deprecated
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
}
