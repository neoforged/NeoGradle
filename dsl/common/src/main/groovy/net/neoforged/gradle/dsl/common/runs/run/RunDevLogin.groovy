package net.neoforged.gradle.dsl.common.runs.run

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Defines the object structure for the dev login configuration of a run
 */
@CompileStatic
interface RunDevLogin extends ConfigurableDSLElement<RunDevLogin> {

    /**
     * Indicates if the dev login is enabled.
     * Its default value is dependent on {@link net.neoforged.gradle.dsl.common.extensions.subsystems.DevLogin#getConventionForRun}.
     *
     * @return {@code true} if the dev login is enabled; otherwise, {@code false}.
     */
    @Input
    @Optional
    @DSLProperty
    Property<Boolean> getIsEnabled()

    /**
     * This is the profile identifier that is used when launching the game through dev login.
     * It is used as the key for looking up credentials in the credentials file, or for creating a new profile.
     */
    @Input
    @Optional
    @DSLProperty
    Property<String> getProfile()
}