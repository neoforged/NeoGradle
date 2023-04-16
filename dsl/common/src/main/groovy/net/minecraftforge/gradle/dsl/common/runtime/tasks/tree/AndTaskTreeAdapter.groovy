package net.minecraftforge.gradle.dsl.common.runtime.tasks.tree

import groovy.transform.PackageScope
import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput
import net.minecraftforge.gradle.dsl.common.util.GameArtifact
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

import java.util.function.Consumer

/**
 * Defines a task tree adapter which applies a given task to a given task provider.
 */
@PackageScope
class AndTaskTreeAdapter implements TaskTreeAdapter {

    private final TaskTreeAdapter left
    private final TaskTreeAdapter right

    AndTaskTreeAdapter(TaskTreeAdapter left, TaskTreeAdapter right) {
        this.left = left
        this.right = right
    }

    @Override
    TaskProvider<? extends Runtime> adapt(Definition<?> definition, Provider<? extends WithOutput> previousTasksOutput, File runtimeWorkspace, Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifacts, Map<String, String> mappingVersionData, Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler) {
        final TaskProvider<? extends Runtime> currentAdapted = left.adapt(definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler);

        if (currentAdapted != null)
            dependentTaskConfigurationHandler.accept(currentAdapted);

        final TaskProvider<? extends Runtime> afterAdapted = right.adapt(definition, currentAdapted, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler);

        if (currentAdapted != null && afterAdapted != null)
            afterAdapted.configure(task -> task.dependsOn(currentAdapted));

        return afterAdapted;
    }
}
