package net.minecraftforge.gradle.legacy.tasks;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainSpec;

import java.io.File;

@Deprecated
public abstract class NoopLegacyRenameJarInPlace extends DefaultTask {

    @TaskAction
    public void apply() {
    }

    @Optional
    @InputFile
    public abstract RegularFileProperty getMappings();

    @Optional
    @InputFiles
    public abstract ConfigurableFileCollection getExtraMappings();

    @Optional
    @InputFiles
    public abstract ConfigurableFileCollection getLibraries();

    @Optional
    @InputFile
    public abstract RegularFileProperty getInput();

    @Internal
    public String getResolvedVersion() { return "*"; }

    @Input
    public boolean getHasLog() { return true; }

    public void setHasLog(boolean value) { }

    @Optional
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public Provider<File> getToolJar() {
        return getProject().provider(() -> File.createTempFile("dummy", ".jar"));
    }

    @Optional
    @Input
    public abstract Property<String> getTool();

    @Optional
    @Input
    public abstract ListProperty<String> getArgs();


    @Input
    @Optional
    public abstract ListProperty<String> getJvmArgs();

    @Input
    @Optional
    public abstract Property<Boolean> getDebug();

    @Optional
    @InputFiles
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    @Nested
    @Optional
    public abstract Property<JavaLauncher> getJavaLauncher();

    @Internal
    public abstract RegularFileProperty getLogOutput();

    public void setMinimumRuntimeJavaVersion(int version) {
    }

    public void setRuntimeJavaVersion(int version) {
    }

    public void setRuntimeJavaToolchain(JavaToolchainSpec toolchain) {
    }

    public void setRuntimeJavaToolchain(Action<? super JavaToolchainSpec> action) {
    }
}
