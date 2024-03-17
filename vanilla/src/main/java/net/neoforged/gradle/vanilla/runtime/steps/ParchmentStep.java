package net.neoforged.gradle.vanilla.runtime.steps;

import com.google.common.collect.Maps;
import net.neoforged.gradle.common.runtime.tasks.Execute;
import net.neoforged.gradle.common.runtime.tasks.NoopRuntime;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Parchment;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.neoforged.gradle.vanilla.runtime.spec.VanillaRuntimeSpecification;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import static net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension.configureCommonRuntimeTaskParameters;

public class ParchmentStep implements IStep {
    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull File workingDirectory, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {

        final TaskProvider<? extends WithOutput> collectLibraryInformationTask = pipelineTasks.get(CommonRuntimeUtils.buildTaskName(definition, "libraries"));

        return maybeApplyParchment(
                definition.getSpecification(),
                inputProvidingTask,
                workingDirectory,
                collectLibraryInformationTask.flatMap(WithOutput::getOutput)
        );
    }

    private static TaskProvider<? extends Runtime> maybeApplyParchment(VanillaRuntimeSpecification spec,
                                                                       TaskProvider<? extends WithOutput> inputProvidingTask,
                                                                       File vanillaDirectory,
                                                                       Provider<RegularFile> listLibrariesOutput) {
        Project project = spec.getProject();
        Parchment parchment = project.getExtensions().getByType(Subsystems.class).getParchment();
        if (!parchment.getEnabled().get()) {
            return project.getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "applyParchmentNoop"), NoopRuntime.class, task -> {
                task.getInput().set(inputProvidingTask.flatMap(WithOutput::getOutput));
            });
        }

        return project.getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "applyParchment"), Execute.class, task -> {
            // Provide the mappings via artifact
            File mappingFile = ToolUtilities.resolveTool(project, parchment.getParchmentArtifact().get());
            File toolExecutable = ToolUtilities.resolveTool(project, parchment.getToolArtifact().get());

            task.getInputs().file(mappingFile);
            task.getInputs().file(inputProvidingTask.flatMap(WithOutput::getOutput));
            task.getInputs().file(listLibrariesOutput);
            task.getExecutingJar().set(toolExecutable);
            task.getProgramArguments().add(listLibrariesOutput.map(f -> "--libraries-list=" + f.getAsFile().getAbsolutePath()));
            task.getProgramArguments().add("--enable-parchment");
            task.getProgramArguments().add("--parchment-mappings=" + mappingFile.getAbsolutePath());
            task.getProgramArguments().add("--in-format=archive");
            task.getProgramArguments().add("--out-format=archive");
            task.getProgramArguments().add(inputProvidingTask.flatMap(WithOutput::getOutput).map(f -> f.getAsFile().getAbsolutePath()));
            task.getProgramArguments().add("{output}");
            configureCommonRuntimeTaskParameters(task, Maps.newHashMap(), "applyParchment", spec, vanillaDirectory);
        });
    }

    @Override
    public String getName() {
        return "parchment";
    }
}
