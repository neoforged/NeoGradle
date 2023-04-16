package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.util.StringCapitalizationUtils;
import net.minecraftforge.gradle.common.runtime.tasks.AccessTransformer;
import net.minecraftforge.gradle.common.runtime.tasks.NoopRuntime;
import net.minecraftforge.gradle.common.util.CommonRuntimeTaskUtils;
import net.minecraftforge.gradle.dsl.common.extensions.AccessTransformers;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

public class ApplyAccessTransformerStep implements IStep {

    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull File workingDirectory, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {
        final Minecraft minecraftExtension = definition.getSpecification().getProject().getExtensions().getByType(Minecraft.class);
        final AccessTransformers accessTransformerFiles = minecraftExtension.getAccessTransformers();

        if (accessTransformerFiles.getFiles().isEmpty() && (!accessTransformerFiles.getEntries().isPresent() || accessTransformerFiles.getEntries().get().isEmpty())) {
            return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), String.format("apply%sAccessTransformer", StringCapitalizationUtils.capitalize("user"))), NoopRuntime.class, task -> {
                task.getInput().set(inputProvidingTask.flatMap(WithOutput::getOutput));
                task.dependsOn(inputProvidingTask);
            });
        }

        final TaskProvider<? extends AccessTransformer> task = CommonRuntimeTaskUtils.createAccessTransformer(
                definition,
                "user",
                workingDirectory,
                gameArtifactTasks,
                definition.getMappingVersionData(),
                additionalTaskConfigurator,
                new ArrayList<>(accessTransformerFiles.getFiles().getFiles()),
                accessTransformerFiles.getEntries().get()
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
