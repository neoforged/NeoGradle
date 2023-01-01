package net.minecraftforge.gradle.dsl.userdev.extension

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.DSLProperty;
import net.minecraftforge.gradle.dsl.base.BaseDSLElement;
import org.gradle.api.provider.Property;

/**
 * Defines a user dev extension within the confines of a forge-based project.
 */
@CompileStatic
interface UserDev extends BaseDSLElement<UserDev> {

    /**
     * Defines the default forge version to use for the project.
     *
     * @return The default forge version to use for the project.
     */
    @DSLProperty
    Property<String> getDefaultForgeVersion();
}
