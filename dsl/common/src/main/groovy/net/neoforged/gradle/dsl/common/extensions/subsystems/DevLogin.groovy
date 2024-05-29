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
     * @return Whether or not to add the dev login repository to the project.
     */
    @Input
    @Optional
    @DSLProperty
    Property<Boolean> getAddRepository();
}