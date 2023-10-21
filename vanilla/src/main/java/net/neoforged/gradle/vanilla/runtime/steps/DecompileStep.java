package net.neoforged.gradle.vanilla.runtime.steps;

import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.DecompileUtils;
import net.neoforged.gradle.common.runtime.tasks.Execute;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.Constants;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.neoforged.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class DecompileStep implements IStep {

    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull File workingDirectory, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {
        final VanillaRuntimeExtension vanillaRuntimeExtension = definition.getSpecification().getProject().getExtensions().getByType(VanillaRuntimeExtension.class);

        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition, "decompile"), Execute.class, task -> {
            final String decompiler = vanillaRuntimeExtension.getVineFlowerVersion().map(version -> String.format(Constants.VINEFLOWER_ARTIFACT_INTERPOLATION, version)).get();
            
            task.getExecutingJar().set(ToolUtilities.resolveTool(task.getProject(), decompiler));
            task.getJvmArguments().addAll(DecompileUtils.DEFAULT_JVM_ARGS);
            task.getProgramArguments().addAll(DecompileUtils.DEFAULT_PROGRAMM_ARGS);
            CommonRuntimeUtils.buildArguments(task.getArguments(), definition, DecompileUtils.DEFAULT_DECOMPILE_VALUES, pipelineTasks, task, Optional.of(inputProvidingTask));
        });
    }

    @Override
    public String getName() {
        return "decompile";
    }
}
