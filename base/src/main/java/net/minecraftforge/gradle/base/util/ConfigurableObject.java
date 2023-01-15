package net.minecraftforge.gradle.base.util;

import groovy.lang.GroovyObjectSupport;
import net.minecraftforge.gradle.dsl.base.BaseDSLElement;
import org.gradle.api.plugins.ExtensionContainer;

import javax.inject.Inject;

public abstract class ConfigurableObject<TSelf extends BaseDSLElement<TSelf>> extends GroovyObjectSupport implements BaseDSLElement<TSelf> {

    public ConfigurableObject() {
    }
}
