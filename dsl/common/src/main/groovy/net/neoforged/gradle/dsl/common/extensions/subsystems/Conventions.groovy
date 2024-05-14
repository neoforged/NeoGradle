package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.Configurations
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.IDE
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.Runs
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.SourceSets
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

/**
 * Allows configuration of the individual conventions used by NeoGradle.
 */
@CompileStatic
interface Conventions extends BaseDSLElement<Conventions> {

    /**
     * Global flag to enable or disable the conventions system. If disabled, all conventions will be ignored.
     * Note: You can not configure this via the buildscript and need to use the gradle.properties file.
     */
    @DSLProperty
    Property<Boolean> getIsEnabled();

    /**
     * The configurations conventions used by NeoGradle.
     */
    @Nested
    @DSLProperty
    Configurations getConfigurations();

    /**
     * The source set conventions used by NeoGradle.
     */
    @Nested
    @DSLProperty
    SourceSets getSourceSets();

    /**
     * The IDE conventions used by NeoGradle.
     */
    @Nested
    @DSLProperty
    IDE getIde();

    /**
     * The runs conventions used by NeoGradle.
     */
    @Nested
    @DSLProperty
    Runs getRuns()
}
