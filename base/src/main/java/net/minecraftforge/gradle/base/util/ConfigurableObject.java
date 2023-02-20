package net.minecraftforge.gradle.base.util;

import groovy.lang.GroovyObjectSupport;
import net.minecraftforge.gradle.dsl.base.BaseDSLElement;

/**
 * A base class for all objects that can be configured via the DSL.
 * @param <TSelf> The type of the implementing class.
 */
public abstract class ConfigurableObject<TSelf extends BaseDSLElement<TSelf>> extends GroovyObjectSupport implements BaseDSLElement<TSelf> {

    public ConfigurableObject() {
    }
}
