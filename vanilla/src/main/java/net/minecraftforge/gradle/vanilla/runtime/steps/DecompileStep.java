package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.common.runtime.tasks.Execute;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.DecompileUtils;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.minecraftforge.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class DecompileStep implements IStep {

    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {
        final VanillaRuntimeExtension vanillaRuntimeExtension = definition.spec().configureProject().getExtensions().getByType(VanillaRuntimeExtension.class);

        return definition.spec().project().getTasks().register(CommonRuntimeUtils.buildTaskName(definition, "decompile"), Execute.class, task -> {
            task.getExecutingArtifact().set(vanillaRuntimeExtension.getForgeFlowerVersion().map(version -> String.format(Utils.FORGEFLOWER_ARTIFACT_INTERPOLATION, version)));
            task.getJvmArguments().addAll(DecompileUtils.DEFAULT_JVM_ARGS);
            task.getProgramArguments().addAll(DecompileUtils.DEFAULT_PROGRAMM_ARGS);
            task.getArguments().set(CommonRuntimeUtils.buildArguments(definition, Collections.emptyMap(), pipelineTasks, task, Optional.of(inputProvidingTask)));
        });
    }

    @Override
    public String getName() {
        return "decompile";
    }
}
