package net.neoforged.gradle.dsl.common.runs.run

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

/**
 * A manager for all runs, is exposed to the script as NamedDomainObjectContainer<Run>
 * <p>
 *     Its management functions are exposed to also handle none registered internal runs properly.
 * </p>
 */
interface RunManager extends NamedDomainObjectContainer<Run> {

    /**
     * Method to add an internal run to the manager, this is a run not registered to the collection,
     * but for which all management callbacks are still invoked.
     *
     * @param run The run to add
     */
    void addInternal(Run run);

    /**
     * Method to register a callback which gets called for all internal and public runs.
     * <p>
     *     This method will realise all public runs and then call the callback for all internal runs.
     * </p>
     *
     * @param forAll The callback to call for all runs
     * @implNote This method is equivalent to {@link NamedDomainObjectContainer#all(Action)} for public runs.
     */
    void realizeAll(Action<Run> forAll)

    /**
     * Method to register a callback which gets called for all runs lazily.
     * <p>
     *     This method will call the callback for all runs, not realizing the runs until they are needed.
     * </p>
     *
     * @param forAll The callback to call for all internal runs
     * @implNote This method is equivalent to {@link NamedDomainObjectContainer#configureEach(Action)} for public runs.
     */
    void configureAll(Action<Run> configure)
}