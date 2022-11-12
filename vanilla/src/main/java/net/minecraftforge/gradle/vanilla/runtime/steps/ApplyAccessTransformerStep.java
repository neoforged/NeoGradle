package net.minecraftforge.gradle.vanilla.runtime.steps;

import net.minecraftforge.gradle.common.extensions.FilesWithEntriesExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.common.runtime.tasks.AccessTransformer;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ApplyAccessTransformerStep implements IStep {

    @Override
    public TaskProvider<? extends IRuntimeTask> buildTask(VanillaRuntimeSpec spec, TaskProvider<? extends IRuntimeTask> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends IRuntimeTask>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends IRuntimeTask>> additionalTaskConfigurator) {
        final VanillaRuntimeExtension vanillaRuntimeExtension = spec.configureProject().getExtensions().getByType(VanillaRuntimeExtension.class);
        final MinecraftExtension minecraftExtension = spec.configureProject().getExtensions().getByType(MinecraftExtension.class);
        final FilesWithEntriesExtension accessTransformers = minecraftExtension.getAccessTransformers();

        return spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, String.format("apply%sAccessTransformer", Utils.capitalize(spec.name()))), AccessTransformer.class, task -> {
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
