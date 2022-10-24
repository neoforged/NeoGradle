package net.minecraftforge.gradle.mcp.runtime.tasks;

import net.minecraftforge.gradle.common.tasks.ITaskWithJavaVersion;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.tasks.ITaskWithWorkspace;
import net.minecraftforge.gradle.common.util.ArtifactSide;
import net.minecraftforge.gradle.mcp.util.CacheableMinecraftVersion;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;

import java.io.File;

/**
 * Defines the structure of a task which is run as part of an mcp runtime execution.
 * By default, it has an output.
 */
public interface IMcpRuntimeTask extends ITaskWithOutput, ITaskWithWorkspace, ITaskWithJavaVersion {

    /**
     * The mcp directory, it is the location of the mcp working directory.
     * @return The mcp working directory.
     */
    @Internal
    DirectoryProperty getMcpDirectory();

    /**
     * The unpacked mcp directory in the global cache.
     * @return The unpacked mcp directory.
     */
    @Internal
    DirectoryProperty getUnpackedMcpZipDirectory();

    /**
     * The steps directory, it is the location of the steps working directory.
     *
     * @return The steps directory.
     */
    @Internal
    DirectoryProperty getStepsDirectory();

    /**
     * The name of the step.
     * @return The name of the step.
     */
    @Input
    Property<String> getStepName();

    /**
     * The requested distribution.
     *
     * @return The requested distribution.
     */
    @Input
    Property<ArtifactSide> getDistribution();

    /**
     * The requested minecraft version.
     *
     * @return The requested minecraft version.
     */
    @Nested
    Property<CacheableMinecraftVersion> getMinecraftVersion();

    /**
     * The custom pipeline file pointer data.
     *
     * @return The custom pipeline file pointer data.
     */
    @Input
    MapProperty<String, File> getData();

    /**
     * The arguments for this step.
     *
     * @return The arguments for this step.
     */
    @Input
    MapProperty<String, Provider<String>> getArguments();

    /**
     * The name of the output file name for this step.
     *
     * @return The name of the output file.
     */
    @Input
    Property<String> getOutputFileName();

    /**
     * The output directory for this step, also doubles as working directory for this step.
     *
     * @return The output and working directory for this step.
     */
    @Internal
    DirectoryProperty getOutputDirectory();
}
