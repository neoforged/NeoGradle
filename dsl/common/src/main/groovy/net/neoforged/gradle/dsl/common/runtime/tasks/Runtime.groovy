package net.neoforged.gradle.dsl.common.runtime.tasks

import groovy.transform.CompileStatic
import net.neoforged.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.tasks.WithJavaVersion
import net.neoforged.gradle.dsl.common.tasks.WithOutput
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace
import net.neoforged.gradle.dsl.common.util.DistributionType
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
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
trait Runtime implements WithOutput, WithWorkspace, WithJavaVersion, AsPartOfStep {

    /**
     * The runtime name, it is the overarching identifier of the runtime that this task is
     * part of.
     * @return The runtime name
     */
    @Internal
    abstract Property<String> getRuntimeName();

    /**
     * The unpacked mcp directory in the global cache.
     * @return The unpacked mcp directory.
     */
    @Internal
    abstract ConfigurableFileCollection getNeoFormArchive();

    /**
     * The requested distribution.
     *
     * @return The requested distribution.
     */
    @Input
    @DSLProperty
    abstract Property<DistributionType> getDistribution();

    /**
     * The requested minecraft version.
     *
     * @return The requested minecraft version.
     */
    @Input
    @DSLProperty
    abstract Property<String> getMinecraftVersion();

    /**
     * Symbolic data source references defined by the NeoForm package, already adjusted for the
     * current {@link #getDistribution()}.
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
