package net.minecraftforge.gradle.dsl.common.tasks

import net.minecraftforge.gradle.dsl.annotations.DSLProperty
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher

/**
 * Defines a task with a java version property.
 */
interface WithJavaVersion extends Task {

    /**
     * The java version to use for this task.
     *
     * @return The java version to use for this task.
     */
    @Input
    @Nested
    @DSLProperty
    Property<JavaLanguageVersion> getJavaVersion();

    /**
     * The java launcher to use for this task.
     *
     * @return The java launcher to use for this task.
     */
    @Internal
    Property<JavaLauncher> getJavaLauncher();
}
