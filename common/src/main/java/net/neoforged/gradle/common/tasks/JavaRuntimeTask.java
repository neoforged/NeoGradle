package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.tasks.WithJavaVersion;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.internal.CurrentJvmToolchainSpec;

import java.io.File;

public abstract class JavaRuntimeTask extends DownloadingTask implements WithJavaVersion {

    public JavaRuntimeTask() {
        getJavaVersion().convention(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
        getJavaLauncher().convention(getJavaToolChain().flatMap(toolChain -> {
            if (!getJavaVersion().isPresent()) {
                return toolChain.launcherFor(new CurrentJvmToolchainSpec(getProject().getObjects()));
            }

            return toolChain.launcherFor(spec -> {
                spec.getLanguageVersion().set(getJavaVersion());
            });
        }));
    }

    @Internal
    public final Provider<JavaToolchainService> getJavaToolChain() {
        return getProject().provider(() -> getProject().getExtensions().getByType(JavaToolchainService.class));
    }

    @Nested
    @Optional
    @Override
    public abstract Property<JavaLanguageVersion> getJavaVersion();

    @Internal
    @Override
    public abstract Property<JavaLauncher> getJavaLauncher();

    @Internal
    public Provider<String> getExecutablePath() {
        return getJavaLauncher().map(JavaLauncher::getExecutablePath).map(RegularFile::getAsFile).map(File::getAbsolutePath);
    }
}
