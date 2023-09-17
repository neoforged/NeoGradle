package net.neoforged.gradle.vanilla.runtime.steps;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.RenameConstants;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class RenameStep implements IStep {

    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull File workingDirectory, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {
        final Mappings mappingsExtension = definition.getSpecification().getProject().getExtensions().getByType(Mappings.class);
        final Map<String, String> mappingVersionData = Maps.newHashMap();
        mappingVersionData.put(NamingConstants.Version.VERSION, definition.getSpecification().getMinecraftVersion());
        mappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, definition.getSpecification().getMinecraftVersion());
        mappingVersionData.putAll(mappingsExtension.getVersion().get());

        final TaskProvider<? extends WithOutput> artifact = inputProvidingTask;

        final Set<TaskProvider<? extends Runtime>> additionalTasks = Sets.newHashSet();
        final TaskBuildingContext context = new TaskBuildingContext(
                definition.getSpecification().getProject(), "mapGame", taskName -> CommonRuntimeUtils.buildTaskName(definition.getSpecification(), taskName), artifact, definition.getGameArtifactProvidingTasks(), mappingVersionData, additionalTasks, definition
        );

        final TaskProvider<? extends Runtime> namingTask = context.getNamingChannel().getJarDeobfuscatingTaskBuilder().get().build(context);
        additionalTasks.forEach(additionalTaskConfigurator);

        namingTask.configure(
                task -> {
                    task.getArguments().putAll(CommonRuntimeUtils.buildArguments(definition, RenameConstants.DEFAULT_RENAME_VALUES, pipelineTasks, task, Optional.of(artifact)));
                    task.getOutput().set(task.getOutputDirectory().file("output.jar"));
                }
        );

        return namingTask;
    }

    @Override
    public String getName() {
        return "rename";
    }
}
