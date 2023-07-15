package net.neoforged.gradle.dsl.common.tasks.specifications

import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher

/**
 * Defines an object which has parameters related to java version management.
 */
trait JavaVersionSpecification implements ProjectSpecification {
    /**
     * The java version to use for this task.
     *
     * @return The java version to use for this task.
     */
    @Input
    @Nested
    @DSLProperty
    abstract Property<JavaLanguageVersion> getJavaVersion();

    /**
     * The java launcher to use for this task.
     *
     * @return The java launcher to use for this task.
     */
    @Internal
    abstract Property<JavaLauncher> getJavaLauncher();

}