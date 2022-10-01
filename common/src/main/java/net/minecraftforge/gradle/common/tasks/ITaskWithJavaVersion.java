package net.minecraftforge.gradle.common.tasks;

import org.gradle.api.provider.Property;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

/**
 * Defines a task with a java version property.
 */
public interface ITaskWithJavaVersion {

    Property<JavaLanguageVersion> getJavaVersion();
}
