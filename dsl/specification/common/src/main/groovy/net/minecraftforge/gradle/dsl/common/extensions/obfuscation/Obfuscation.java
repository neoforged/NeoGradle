package net.minecraftforge.gradle.dsl.common.extensions.obfuscation;

import net.minecraftforge.gradle.dsl.annotations.DSLProperty;
import net.minecraftforge.gradle.dsl.base.BaseDSLElement;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a project component which manages the configuration of the obfuscation logic.
 */
public interface Obfuscation extends BaseDSLElement<Obfuscation> {


    /**
     * Indicates if the system should automatically obfuscate all Jar tasks.
     *
     * @return The indicator if the system should obfuscate all jar tasks automatically.
     */
    @NotNull
    @DSLProperty
    Property<Boolean> getCreateAutomatically();
}
