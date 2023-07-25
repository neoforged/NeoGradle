package net.neoforged.gradle.dsl.common.runtime.naming;

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement;
import net.minecraftforge.gdi.annotations.DSLProperty;
import org.gradle.api.provider.Property
import org.jetbrains.annotations.NotNull;

/**
 * Defines a channel for a naming scheme.
 * Handles the creation of tasks for applying and un-applying mappings to a jar being it a sources or compiled jar.
 */
@CompileStatic
interface NamingChannel extends BaseDSLElement<NamingChannel> {

    /**
     * The name of the naming channel.
     *
     * @return The name.
     */
    @NotNull
    String getName();

    /**
     * The extractor which can extract the minecraft version from the current mapping channel.
     *
     * @return The extractor.
     */
    @DSLProperty
    Property<MinecraftVersionFromVersionDataProducer> getMinecraftVersionExtractor();

    /**
     * The builder which can construct a new task provider for a source jar mapping.
     * Every time this is invoked the builder needs to produce a new task.
     * However, the builder is allowed to reuse an old task if the inputs match.
     *
     * @return The builder property.
     */
    @DSLProperty
    Property<ApplyMappingsToSourceJarTaskBuilder> getApplySourceMappingsTaskBuilder();

    /**
     * The builder which can construct a new task provider for a compiled jar mapping.
     * Every time this is invoked the builder needs to produce a new task.
     * However, the builder is allowed to reuse an old task if the inputs match.
     *
     * @return The builder property.
     */
    @DSLProperty
    Property<ApplyMappingsToCompiledJarTaskBuilder> getApplyCompiledMappingsTaskBuilder();

    /**
     * The builder which can construct a new task provider for a compiled jar unmapping.
     * Every time this is invoked the builder needs to produce a new task.
     * However, the builder is allowed to reuse an old task if the inputs match.
     *
     * @return The builder property.
     */
    @DSLProperty
    Property<UnapplyMappingsToCompiledJarTaskBuilder> getUnapplyCompiledMappingsTaskBuilder();

    /**
     * The builder which can construct a new task provider for a access transformer unmapping.
     * Every time this is invoked the builder needs to produce a new task.
     * However, the builder is allowed to reuse an old task if the inputs match.
     *
     * @return The builder property.
     */
    @DSLProperty
    Property<UnapplyMappingsToAccessTransformerTaskBuilder> getUnapplyAccessTransformerMappingsTaskBuilder();

    /**
     * The builder which can generate a task provider that creates the "mcp" mappings jar.
     * This "mcp" mappings jar is used during "debugging", basically when the user is running the game in their IDE.
     * The jar needs to contain two CSV files in the root of the jar:
     * - methods.csv
     * - fields.csv
     *
     * The CSV files need to be in the following format:
     * - methods.csv: ""searge", "name", "side", "desc""
     * - fields.csv: ""searge", "name", "side", "desc""
     *
     * See the following example of the legacy code that generated the file in FG5:
     * https://github.com/MinecraftForge/ForgeGradle/blob/ce931cc5c05c935b7b48bce580334abca21840dd/src/mcp/java/net/minecraftforge/gradle/mcp/OfficialChannelProvider.java#L126
     * @return The builder property.
     */
    @DSLProperty
    Property<GenerateDebuggingMappingsJarTaskBuilder> getGenerateDebuggingMappingsJarTaskBuilder();

    /**
     * Returns the group prefix for the current channel that is prefixed to the deobfuscated dependency groups.
     *
     * @return The group prefix.
     */
    @DSLProperty
    Property<String> getDeobfuscationGroupSupplier();

    /**
     * @return Indicates if the user has accepted this mappings license.
     */
    @DSLProperty
    Property<Boolean> getHasAcceptedLicense();

    /**
     * @return The license text of this mappings channel.
     */
    @DSLProperty
    Property<String> getLicenseText();

    /**
     * @return The dependency notation version manager.
     */
    @DSLProperty
    Property<DependencyNotationVersionManager> getDependencyNotationVersionManager();
}
