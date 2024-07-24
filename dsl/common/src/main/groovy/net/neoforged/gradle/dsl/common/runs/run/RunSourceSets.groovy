package net.neoforged.gradle.dsl.common.runs.run

import com.google.common.collect.Multimap
import groovy.transform.CompileStatic
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Action
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.SourceSet

/**
 * Represents the source sets attached to a run for a specific reason (e.g. testing, mods, etc.)
 */
@CompileStatic
interface RunSourceSets extends ConfigurableDSLElement<RunSourceSets> {

    /**
     * Adds a source set to this run
     * The group id used is derived from the project id of the source set if it is not set.
     *
     * @param sourceSet The source set to add
     */
    void add(SourceSet sourceSet);

    /**
     * Adds multiple source sets to this run
     * The group id used is derived from the project id of the source set if it is not set.
     *
     * @param sourceSets The source sets to add
     */
    void add(Iterable<? extends SourceSet> sourceSets);

    /**
     * Adds multiple source sets to this run
     * The group id used is derived from the project id of the source set if it is not set.
     *
     * @param sourceSets The source sets to add
     */
    void add(SourceSet... sourceSets);

    /**
     * Adds a source set to this run
     * The group id used is derived from the local project name if it is not set.
     *
     * @param sourceSet The source set to add
     */
    void local(SourceSet sourceSet);

    /**
     * Adds multiple source sets to this run
     * The group id used is derived from the local project name if it is not set.
     *
     * @param sourceSets The source sets to add
     */
    void local(Iterable<? extends SourceSet> sourceSets);

    /**
     * Adds multiple source sets to this run
     * The group id used is derived from the local project name if it is not set.
     *
     * @param sourceSets The source sets to add
     */
    void local(SourceSet... sourceSets);

    /**
     * Adds a source set to this run
     *
     * @param groupId The group ID of the source set
     * @param sourceSet The source set to add
     */
    void add(String groupId, SourceSet sourceSet);

    /**
     * Adds multiple source sets to this run
     *
     * @param groupId The group ID of the source sets
     * @param sourceSets The source sets to add
     */
    void add(String groupId, Iterable<? extends SourceSet> sourceSets);

    /**
     * Adds multiple source sets to this run
     *
     * @param groupId The group ID of the source sets
     * @param sourceSets The source sets to add
     */
    void add(String groupId, SourceSet... sourceSets);

    /**
     * Defines the primary sourceset of the run,
     * If defined as to be part of the run, it will be used as the primary source set, who's minecraft dependency will be used
     * instead of the default combined runtime classpath of the mod sources.
     *
     * This can fix issues with the newer FML 3.0.0+ where there can only be one minecraft dependency.
     *
     * @return The primary source set
     */
    @DSLProperty
    @Input
    @Optional
    Property<SourceSet> getPrimary();

    /**
     * The source sets attached to this run
     */
    @Internal
    Provider<Multimap<String, SourceSet>> all();

    /**
     * Registers a callback that get called when a source set is added to this run
     *
     * @param action The action to call
     */
    void whenSourceSetAdded(Action<SourceSet> action)
}
