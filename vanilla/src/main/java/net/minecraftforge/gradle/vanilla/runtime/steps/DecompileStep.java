package net.minecraftforge.gradle.vanilla.runtime.steps;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.minecraftforge.gradle.common.runtime.tasks.Execute;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class DecompileStep implements IStep {

    private static final List<String> DEFAULT_JVM_ARGS = ImmutableList.of("-Xmx4g");
    private static final List<String> DEFAULT_PROGRAMM_ARGS = ImmutableList.<String>builder()
            .add(
                    "-din=1",
                    "-rbr=1",
                    "-dgs=1",
                    "-asc=1",
                    "-rsy=1",
                    "-iec=1",
                    "-jvn=1",
                    "-isl=0",
                    "-iib=1",
                    "-bsm=1",
                    "-dcl=1",
                    "-log=TRACE",
                    "-cfg",
                    "{libraries}",
                    "{input}",
                    "{output}"
            ).build();

    @Override
    public TaskProvider<? extends IRuntimeTask> buildTask(VanillaRuntimeSpec spec, TaskProvider<? extends IRuntimeTask> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends IRuntimeTask>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends IRuntimeTask>> additionalTaskConfigurator) {
        final VanillaRuntimeExtension vanillaRuntimeExtension = spec.configureProject().getExtensions().getByType(VanillaRuntimeExtension.class);

        return spec.project().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "decompile"), Execute.class, task -> {
            task.getExecutingArtifact().set(vanillaRuntimeExtension.getForgeFlowerVersion().map(version -> String.format(Utils.FORGEFLOWER_VERSION_INTERPOLATION, version)));
            task.getJvmArguments().addAll(DEFAULT_JVM_ARGS);
            task.getProgramArguments().addAll(DEFAULT_PROGRAMM_ARGS);
            task.getArguments().set(CommonRuntimeUtils.buildArguments(spec, Collections.emptyMap(), pipelineTasks, task, Optional.of(inputProvidingTask)));
        });
    }

    @Override
    public String getName() {
        return "decompile";
    }
}
