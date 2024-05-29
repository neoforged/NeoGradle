package net.neoforged.gradle.dsl.common.extensions.subsystems

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Allows configuration of the dev login system.
 */
@CompileStatic
interface DevLogin extends ConfigurableDSLElement<DevLogin> {

    /**
     * @return Whether or not dev login is enabled on launch.
     */
    @Input
    @Optional
    @DSLProperty
    Property<Boolean> getEnabled();

    /**
     * @return The main class to use when launching the game through dev login.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getMainClass();
}