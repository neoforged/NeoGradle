package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.dsl.common.extensions.JarJarFeature;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Attribute;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class JarJarExtension extends DefaultJarJarFeature implements net.neoforged.gradle.dsl.common.extensions.JarJar {
    public static final Attribute<String> JAR_JAR_RANGE_ATTRIBUTE = Attribute.of("jarJarRange", String.class);
    public static final Attribute<String> FIXED_JAR_JAR_VERSION_ATTRIBUTE = Attribute.of("fixedJarJarVersion", String.class);

    private final Map<String, DefaultJarJarFeature> features = new HashMap<>();

    @Inject
    public JarJarExtension(final Project project) {
        super(project, "");
        features.put("", this);
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

    public JarJarFeature forFeature(String featureName) {
        if (featureName == null || featureName.isEmpty()) {
            return this;
        }
        return features.computeIfAbsent(featureName, f -> {
            DefaultJarJarFeature feature = new DefaultJarJarFeature(project, f);
            feature.createTaskAndConfiguration();
            return feature;
        });
    }
}
