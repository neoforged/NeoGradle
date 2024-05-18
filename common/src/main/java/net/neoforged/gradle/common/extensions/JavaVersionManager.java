package net.neoforged.gradle.common.extensions;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.internal.JavaToolchain;

import javax.inject.Inject;

public class JavaVersionManager {

    private final Project project;
    private int javaVersion = -1;

    @Inject
    public JavaVersionManager(Project project) {
        this.project = project;
    }

    public void setJavaVersion(int javaVersion, String context) {
        if (this.javaVersion > javaVersion) {
            project.getLogger().warn("Can not reconfigure java version from {} to {} for {}", this.javaVersion, javaVersion, context);
            return;
        }

        this.javaVersion = javaVersion;
        project.getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(javaVersion));
    }
}
