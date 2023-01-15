package net.minecraftforge.gradle.common.util;

import net.minecraftforge.gradle.common.runtime.tasks.AccessTransformer;
import net.minecraftforge.gradle.common.runtime.tasks.AccessTransformerFileGenerator;
import net.minecraftforge.gradle.dsl.base.util.GameArtifact;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.ArtifactProvider;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class CommonRuntimeTaskUtils {

    private CommonRuntimeTaskUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: CommonRuntimeTaskUtils. This is a utility class");
    }

    public static TaskProvider<? extends AccessTransformer> createAccessTransformer(Definition<?> definition, String namePreFix, File workspaceDirectory, Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTaskProviderMap, Map<String, String> versionData, Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler, List<File> files, Collection<String> data) {
        final Collection<TaskProvider<? extends WithOutput>> fileRemapTasks = new ArrayList<>();
        for (File file : files) {
            final TaskProvider<? extends WithOutput> provider = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), namePreFix + "AccessTransformerProvider" + file.getName()), ArtifactProvider.class, task -> {
                task.getInput().set(file);
                task.getOutput().set(new File(workspaceDirectory, "accesstransformers/" + file.getName()));
            });

            generateAccessTransformerRemapTask(definition, gameArtifactTaskProviderMap, versionData, dependentTaskConfigurationHandler, fileRemapTasks, provider);
        }

        if (!data.isEmpty()) {
            final TaskProvider<AccessTransformerFileGenerator> generator = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), namePreFix + "AccessTransformerGenerator"), AccessTransformerFileGenerator.class, task -> {
                task.getOutput().set(new File(workspaceDirectory, "accesstransformers/_script-access-transformer.cfg"));
                task.getAdditionalTransformers().set(data);
            });
            dependentTaskConfigurationHandler.accept(generator);
            generateAccessTransformerRemapTask(definition, gameArtifactTaskProviderMap, versionData, dependentTaskConfigurationHandler, fileRemapTasks, generator);
        }

        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), String.format("apply%sAccessTransformer", net.minecraftforge.gradle.base.util.StringUtils.capitalize(namePreFix))), AccessTransformer.class, task -> {
            for (TaskProvider<? extends WithOutput> fileRemapTask : fileRemapTasks) {
                task.getTransformers().from(fileRemapTask.flatMap(WithOutput::getOutput));
                task.dependsOn(fileRemapTask);
            }
        });
    }

    private static void generateAccessTransformerRemapTask(Definition<?> definition, Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactTaskProviderMap, Map<String, String> versionData, Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler, Collection<TaskProvider<? extends WithOutput>> fileRemapTasks, TaskProvider<? extends WithOutput> provider) {
        final Set<TaskProvider<? extends Runtime>> additionalRemapTasks = new HashSet<>();
        final TaskBuildingContext context = new TaskBuildingContext(
                definition.getSpecification().getProject(),
                "accessTransform" + StringUtils.capitalize(definition.getSpecification().getName()),
                name -> CommonRuntimeUtils.buildTaskName(definition.getSpecification(), name),
                provider,
                gameArtifactTaskProviderMap,
                versionData,
                additionalRemapTasks,
                definition
        );

        final TaskProvider<? extends Runtime> remapTask = definition.getSpecification().getProject().getExtensions().getByType(Mappings.class).getChannel().get()
                        .getUnapplyAccessTransformerMappingsTaskBuilder()
                                .get().build(context);

        remapTask.configure(task -> task.dependsOn(provider));

        additionalRemapTasks.forEach(dependentTaskConfigurationHandler);
        dependentTaskConfigurationHandler.accept(remapTask);

        fileRemapTasks.add(remapTask);
    }
}
