package net.minecraftforge.gradle.common.tasks;

import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;

public abstract class JavaRuntimeTask extends DownloadingTask implements ITaskWithJavaVersion {

    public JavaRuntimeTask() {
        getJavaVersion().convention(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
    }

    @Internal
    public final Provider<JavaToolchainService> getJavaToolChain() {
        return getProject().provider(() -> getProject().getExtensions().getByType(JavaToolchainService.class));
    }

    @Nested
    @Override
    public abstract Property<JavaLanguageVersion> getJavaVersion();

    @Internal
    protected Provider<String> getExecutablePath() {
        return getJavaToolChain()
                .flatMap(toolChain -> getJavaVersion().flatMap(javaVersion -> toolChain.launcherFor(spec -> spec.getLanguageVersion().set(javaVersion)))
                        .map(launcher -> launcher.getExecutablePath().getAsFile().getAbsolutePath()));

    }
}
