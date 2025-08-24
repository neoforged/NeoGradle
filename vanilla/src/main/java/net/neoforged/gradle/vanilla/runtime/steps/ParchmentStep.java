package net.neoforged.gradle.vanilla.runtime.steps;

import com.google.common.collect.Maps;
import net.neoforged.gradle.common.extensions.subsystems.SubsystemsExtension;
import net.neoforged.gradle.common.runtime.tasks.NoopRuntime;
import net.neoforged.gradle.common.util.JavaSourceTransformAdapterUtils;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Parchment;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import static net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension.configureCommonRuntimeTaskParameters;

public class ParchmentStep implements IStep {
    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull File workingDirectory, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {
        Project project = definition.getSpecification().getProject();
        Parchment parchment = project.getExtensions().getByType(Subsystems.class).getParchment();

        @Nullable
        final TaskProvider<? extends Runtime> transformerTask = JavaSourceTransformAdapterUtils.createJavaSourceTransformerTask(
            definition.getSpecification().getProject().files(),
            definition.getSpecification().getProject().files(),
            definition,
            inputProvidingTask,
            additionalTaskConfigurator,
            (SubsystemsExtension.ParchmentExtensions) parchment
        );

        if (transformerTask == null) {
            return project.getTasks().register(CommonRuntimeUtils.buildTaskName(definition, "applyParchmentNoop"), NoopRuntime.class, task -> {
                task.getInput().set(inputProvidingTask.flatMap(WithOutput::getOutput));
            });
        }

        transformerTask.configure(task -> {
            configureCommonRuntimeTaskParameters(task, Maps.newHashMap(), "applyParchment", definition.getSpecification(), workingDirectory);
        });

        return transformerTask;
    }

    @Override
    public String getName() {
        return "parchment";
    }
}
