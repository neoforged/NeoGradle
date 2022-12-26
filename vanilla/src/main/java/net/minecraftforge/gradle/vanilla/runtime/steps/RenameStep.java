package net.minecraftforge.gradle.vanilla.runtime.steps;

import com.google.common.collect.Sets;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class RenameStep implements IStep {

    @Override
    public TaskProvider<? extends Runtime> buildTask(VanillaRuntimeDefinition definition, TaskProvider<? extends WithOutput> inputProvidingTask, @NotNull File minecraftCache, @NotNull Map<String, TaskProvider<? extends WithOutput>> pipelineTasks, @NotNull Map<GameArtifact, File> gameArtifacts, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTasks, @NotNull Consumer<TaskProvider<? extends Runtime>> additionalTaskConfigurator) {
        final Mappings mappingsExtension = definition.spec().getProject().getExtensions().getByType(Mappings.class);
        final Map<String, String> mappingVersionData = mappingsExtension.getVersion().get();

        final Set<TaskProvider<? extends Runtime>> additionalTasks = Sets.newHashSet();
        final TaskBuildingContext context = new TaskBuildingContext(
                definition.spec().getProject(), "mapGame", inputProvidingTask, definition.gameArtifactProvidingTasks(), mappingVersionData, additionalTasks
                );

        final TaskProvider<? extends Runtime> namingTask = context.getNamingChannel().getApplySourceMappingsTaskBuilder().get().build(context);
        additionalTasks.forEach(additionalTaskConfigurator);

        return namingTask;
    }

    @Override
    public String getName() {
        return "rename";
    }
}
