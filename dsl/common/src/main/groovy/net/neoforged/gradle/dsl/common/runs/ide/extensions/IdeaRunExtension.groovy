package net.neoforged.gradle.dsl.common.runs.ide.extensions

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet

/**
 * An extension added to all runs which allows for the configuration of IDEA specific integrations.
 */
@CompileStatic
interface IdeaRunExtension extends BaseDSLElement<IdeaRunExtension> {

    /**
     * The primary source set to which the reference is made for the run.
     * IDEA pull dependencies and compile information from this.
     *
     * @return The primary source set.
     * @implNote The default value of this is always the main source set of the project that owns the run.
     */
    @DSLProperty
    Property<SourceSet> getPrimarySourceSet();
}