package net.neoforged.gradle.dsl.common.extensions.subsystems.conventions

import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.ide.IDEA
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

/**
 * Allows configuration of the individual conventions related to IDEs used by NeoGradle.
 */
interface IDE extends BaseDSLElement<IDE> {

    /**
     * Global flag to enable or disable the IDE conventions system. If disabled, no conventions IDEs will be created or used.
     * Note: this can not be configured via the buildscript and needs to be set in the gradle.properties file.
     */
    @DSLProperty
    Property<Boolean> getIsEnabled();

    /**
     * The IDEA conventions used by NeoGradle.
     */
    @Nested
    @DSLProperty
    IDEA getIdea();
}