package net.minecraftforge.gradle.common.util;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.GroovyObjectSupport;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import org.gradle.api.Action;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Defines an object which supports configuration using different systems available in Gradle.
 * @param <T> The type of the object, needs to be the target type.
 */
public interface IConfigurableObject<T extends IConfigurableObject<T>> extends Configurable<T>, ExtensionAware {

    /**
     * Returns the current instance cast to the right target.
     * @return The current instance.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    default T getThis() {
        return (T) this;
    }

    /**
     * Configures this object using the given closure.
     * @param closure The closure used to configure the target.
     * @return This object.
     */
    @SuppressWarnings("deprecation") //Use internal variant if ever removed.
    @Override
    @NotNull
    default T configure(Closure closure) {
        return ConfigureUtil.configureSelf(closure, getThis());
    }

    /**
     * Configures this object using the given action.
     * @param consumer The action used to configure the target.
     * @return This object.
     */
    @NotNull
    default T configure(final Consumer<T> consumer) {
        consumer.accept(getThis());
        return getThis();
    }

    /**
     * Configures this object using the given action.
     * @param consumer The action used to configure the target.
     * @return This object.
     */
    @NotNull
    default T configure(final Action<T> consumer) {
        consumer.execute(getThis());
        return getThis();
    }

    /**
     * Configures this object using the given properties map.
     * @param source The source to configure the object from.
     * @return This object.
     */
    @SuppressWarnings("deprecation") //Use internal variant if ever removed.
    @NotNull
    default T configure(final Map<String, Object> source) {
        return ConfigureUtil.configureByMap(source, getThis());
    }
}
