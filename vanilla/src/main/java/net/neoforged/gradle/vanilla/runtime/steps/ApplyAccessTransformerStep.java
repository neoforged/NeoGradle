package net.neoforged.gradle.vanilla.runtime.steps;

import net.neoforged.gradle.common.runtime.tasks.BinaryAccessTransformer;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import net.neoforged.gradle.common.util.CommonRuntimeTaskUtils;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

public class ApplyAccessTransformerStep implements IStep {

    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull File workingDirectory, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {
        final Minecraft minecraftExtension = definition.getSpecification().getProject().getExtensions().getByType(Minecraft.class);
        final AccessTransformers accessTransformerFiles = minecraftExtension.getAccessTransformers();

        final TaskProvider<? extends BinaryAccessTransformer> task = CommonRuntimeTaskUtils.createBinaryAccessTransformer(
                definition,
                "user",
                workingDirectory,
                accessTransformerFiles.getFiles().getAsFileTree()
        );

        task.configure(t -> {
            t.getInputFile().set(inputProvidingTask.flatMap(WithOutput::getOutput));
            t.dependsOn(inputProvidingTask);
        });

        return task;
    }

    @Override
    public String getName() {
        return "applyAccessTransformer";
    }

    @Override
    public String getTaskName(VanillaRuntimeDefinition definition) {
        return CommonRuntimeUtils.buildTaskName(definition.getSpecification(), String.format("apply%sAccessTransformer", StringCapitalizationUtils.capitalize("user")));
    }
}
