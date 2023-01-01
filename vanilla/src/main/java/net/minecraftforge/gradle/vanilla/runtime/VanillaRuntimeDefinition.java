package net.minecraftforge.gradle.vanilla.runtime;

import net.minecraftforge.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.ArtifactProvider;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpecification;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a configured and registered runtime for vanilla.
 */
public final class VanillaRuntimeDefinition extends CommonRuntimeDefinition<VanillaRuntimeSpecification> {

    public VanillaRuntimeDefinition(@NotNull VanillaRuntimeSpecification specification, @NotNull LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs, @NotNull TaskProvider<? extends ArtifactProvider> sourceJarTask, @NotNull TaskProvider<? extends ArtifactProvider> rawJarTask, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactProvidingTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Configuration minecraftDependenciesConfiguration, @NotNull Consumer<TaskProvider<? extends Runtime>> associatedTaskConsumer) {
        super(specification, taskOutputs, sourceJarTask, rawJarTask, gameArtifactProvidingTasks, minecraftDependenciesConfiguration, associatedTaskConsumer);
    }
}
