package net.neoforged.gradle.dsl.userdev.extension

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty;
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
    
    /**
     * Defines the default forge group to use for the project.
     *
     * @return The default forge group to use for the project.
     */
    @DSLProperty
    Property<String> getDefaultForgeGroup();    
    
    /**
     * Defines the default forge artifact name to use for the project.
     *
     * @return The default forge artifact name to use for the project.
     */
    @DSLProperty
    Property<String> getDefaultForgeName();
}
