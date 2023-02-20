package net.minecraftforge.gradle.dsl.base.util

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.BouncerMethod
import net.minecraftforge.gradle.dsl.annotations.DefaultMethods
import net.minecraftforge.gradle.util.GradleInternalUtils
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Internal
import org.gradle.util.Configurable
import org.jetbrains.annotations.NotNull

import java.util.function.Consumer

/**
 * Defines an object which supports configuration using different systems available in Gradle.
 * @param <T> The type of the object, needs to be the target type.
 */
@CompileStatic
@DefaultMethods
trait ConfigurableDSLElement<T extends ConfigurableDSLElement<T>> implements Configurable<T>, ExtensionAware {

    /**
     * Returns the current instance cast to the right target.
     * @return The current instance.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    @Internal
    default T getThis() {
        return (T) this;
    }

    /**
     * Configures this object using the given closure.
     * @param closure The closure used to configure the target.
     * @return This object.
     */
    @NotNull
    @Override
    @SuppressWarnings("deprecation") //Use internal variant if ever removed.
    @BouncerMethod(returnType = Object.class)
    default T configure(Closure closure) {
        return GradleInternalUtils.configureSelf(closure, getThis());
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
        return GradleInternalUtils.configureByMap(source, getThis());
    }
}
