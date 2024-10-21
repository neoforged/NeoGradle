package net.neoforged.gradle.dsl.common.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested

/**
 * Represents the data for injecting interfaces into a target class.
 */
@CompileStatic
abstract class InjectedInterfaceData implements ConfigurableDSLElement<InjectedInterfaceData> {

    /**
     * The binary representation of the target class to inject the interfaces into.
     *
     * @return The target class to inject the interfaces into.
     */
    @Input
    @DSLProperty
    abstract Property<String> getTarget();

    /**
     * The binary representation of the interfaces to inject.
     * <p>
     *     Generics are copied verbatim. If you need the generics to reference a class, please use its fully qualified name (e.g. java/util/function/Supplier<java.util.concurrent.atomic.AtomicInteger>).
     * </p>
     *
     * @return The interfaces to inject.
     */
    @Input
    @DSLProperty
    abstract ListProperty<String> getInterfaces();
}
