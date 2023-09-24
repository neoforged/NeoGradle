package net.neoforged.gradle.dsl.common.runs.type

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.runs.run.Run

@CompileStatic
@FunctionalInterface
interface RunAdapter {

    /**
     * Invoked to configure a run which has the type that owns this adapter.
     *
     * @param run The run adapter.
     */
    void adapt(Run run);
}