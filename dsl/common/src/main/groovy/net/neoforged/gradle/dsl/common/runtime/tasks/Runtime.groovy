package net.neoforged.gradle.dsl.common.runtime.tasks

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.annotations.DSLProperty
import net.neoforged.gradle.dsl.common.tasks.WithJavaVersion
import net.neoforged.gradle.dsl.common.tasks.WithOutput
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace
import net.neoforged.gradle.dsl.common.util.CacheableMinecraftVersion
import net.neoforged.gradle.dsl.common.util.DistributionType
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

/**
 * Defines the structure of a task which is run as part of a runtime execution.
 * By default, it has an output.
 */
@CompileStatic
trait Runtime implements WithOutput, WithWorkspace, WithJavaVersion {

    /**
     * The runtime name, it is the overarching identifier of the runtime that this task is
     * part of.
     * @return The runtime name
     */
    @Internal
    abstract Property<String> getRuntimeName();

    /**
     * The runtime directory, it is the location of the runtime working directory.
     * @return The mcp working directory.
     */
    @Internal
    abstract DirectoryProperty getRuntimeDirectory();

    /**
     * The unpacked mcp directory in the global cache.
     * @return The unpacked mcp directory.
     */
    @Internal
    abstract DirectoryProperty getUnpackedMcpZipDirectory();

    /**
     * The steps directory, it is the location of the steps working directory.
     *
     * @return The steps directory.
     */
    @Internal
    abstract DirectoryProperty getStepsDirectory();

    /**
     * The name of the step.
     * @return The name of the step.
     */
    @Input
    @DSLProperty
    abstract Property<String> getStepName();

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
    @Nested
    @DSLProperty
    abstract Property<CacheableMinecraftVersion> getMinecraftVersion();

    /**
     * The custom pipeline file pointer data.
     *
     * @return The custom pipeline file pointer data.
     */
    @Input
    @DSLProperty
    abstract MapProperty<String, File> getData();

    /**
     * The arguments for this step.
     *
     * @return The arguments for this step.
     */
    @Input
    @DSLProperty
    abstract MapProperty<String, Provider<String>> getArguments();

    /**
     * The multi statement arguments for this step.
     *
     * @return The arguments for this step.
     */
    @Input
    @DSLProperty
    abstract MapProperty<String, Provider<List<String>>> getMultiArguments();

    /**
     * The name of the output file name for this step.
     *
     * @return The name of the output file.
     */
    @Input
    @DSLProperty
    abstract Property<String> getOutputFileName();

    /**
     * The output directory for this step, also doubles as working directory for this step.
     *
     * @return The output and working directory for this step.
     */
    @Internal
    abstract DirectoryProperty getOutputDirectory();
}
