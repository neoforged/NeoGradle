package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.common.extensions.base.BaseFilesWithEntriesExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.common.runtime.tasks.AccessTransformer;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.minecraftforge.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

public class ApplyAccessTransformerStep implements IStep {

    @Override
    public TaskProvider<? extends IRuntimeTask> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends ITaskWithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends ITaskWithOutput>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends IRuntimeTask>> additionalTaskConfigurator) {
        final VanillaRuntimeExtension vanillaRuntimeExtension = definition.spec().configureProject().getExtensions().getByType(VanillaRuntimeExtension.class);
        final MinecraftExtension minecraftExtension = definition.spec().configureProject().getExtensions().getByType(MinecraftExtension.class);
        final BaseFilesWithEntriesExtension accessTransformers = minecraftExtension.getAccessTransformers();

        return definition.spec().project().getTasks().register(CommonRuntimeUtils.buildTaskName(definition, String.format("apply%sAccessTransformer", Utils.capitalize(definition.spec().name()))), AccessTransformer.class, task -> {
            task.getAdditionalTransformers().addAll(accessTransformers.getEntries());
            task.getTransformers().setFrom(accessTransformers.getFiles());
            task.getExecutingArtifact().set(vanillaRuntimeExtension.getAccessTransformerApplierVersion().map(version -> String.format(Utils.ACCESSTRANSFORMER_VERSION_INTERPOLATION, version)));
        });
    }

    @Override
    public String getName() {
        return "applyAccessTransformer";
    }
}
