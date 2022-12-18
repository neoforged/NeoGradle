package net.minecraftforge.gradle.common.tasks;

import groovy.cli.Option;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.internal.jvm.inspection.JvmVendor;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JvmVendorSpec;

/**
 * Defines a task with a java version property.
 */
public interface ITaskWithJavaVersion {

    @Input
    @Optional
    @Nested
    Property<JavaLanguageVersion> getRuntimeJavaVersion();

    @Internal
    Property<JavaLauncher> getRuntimeJavaLauncher();
}
