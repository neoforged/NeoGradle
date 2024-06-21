package net.neoforged.gradle.dsl.common.runtime.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.tasks.WithJavaVersion
import net.neoforged.gradle.dsl.common.tasks.WithOutput
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace
import net.neoforged.gradle.dsl.common.tasks.specifications.RuntimeSpecification
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

/**
 * Defines the structure of a task which is run as part of a runtime execution.
 * By default, it has an output.
 */
@CompileStatic
trait Runtime implements WithOutput, WithWorkspace, WithJavaVersion, RuntimeSpecification {

    /**
     * Symbolic data source references defined by the NeoForm package, already adjusted for the
     * current distribution.
     */
    @Internal
    abstract MapProperty<String, String> getSymbolicDataSources();

    /**
     * The arguments for this step.
     *
     * @return The arguments for this step.
     */
    @Nested
    @DSLProperty
    abstract RuntimeArguments getArguments();

    /**
     * The multi statement arguments for this step.
     *
     * @return The arguments for this step.
     */
    @Nested
    @DSLProperty
    abstract RuntimeMultiArguments getMultiArguments();
}
