package net.minecraftforge.gradle.common.tasks;

import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.internal.jvm.inspection.JvmVendor;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JvmVendorSpec;
import org.gradle.jvm.toolchain.internal.CurrentJvmToolchainSpec;

import java.io.File;

public abstract class JavaRuntimeTask extends DownloadingTask implements ITaskWithJavaVersion {

    public JavaRuntimeTask() {
        getRuntimeJavaVersion().convention(getProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
        getRuntimeJavaLauncher().convention(getJavaToolChain().flatMap(toolChain -> {
            if (!getRuntimeJavaVersion().isPresent()) {
                return toolChain.launcherFor(new CurrentJvmToolchainSpec(getProject().getObjects()));
            }

            return toolChain.launcherFor(spec -> {
                spec.getLanguageVersion().set(getRuntimeJavaVersion());
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
    public abstract Property<JavaLanguageVersion> getRuntimeJavaVersion();

    @Internal
    @Override
    public abstract Property<JavaLauncher> getRuntimeJavaLauncher();

    @Internal
    protected Provider<String> getExecutablePath() {
        return getRuntimeJavaLauncher().map(JavaLauncher::getExecutablePath).map(RegularFile::getAsFile).map(File::getAbsolutePath);
    }
}
