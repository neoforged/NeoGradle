package net.neoforged.gradle.common.util;

import net.neoforged.gradle.common.runtime.tasks.AccessTransformer;
import net.neoforged.gradle.common.runtime.tasks.AccessTransformerFileGenerator;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class CommonRuntimeTaskUtils {

    private CommonRuntimeTaskUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: CommonRuntimeTaskUtils. This is a utility class");
    }

    public static TaskProvider<? extends AccessTransformer> createAccessTransformer(Definition<?> definition, String namePreFix, File workspaceDirectory, Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler, Iterable<File> files, Collection<String> data) {
        final Collection<TaskProvider<? extends WithOutput>> fileProducingTasks = new ArrayList<>();
        for (File file : files) {
            final TaskProvider<? extends WithOutput> provider = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), namePreFix + "AccessTransformerProvider" + file.getName()), ArtifactProvider.class, task -> {
                task.getInput().set(file);
                task.getOutput().set(new File(workspaceDirectory, "accesstransformers/" + namePreFix + "/" + file.getName()));
            });

            fileProducingTasks.add(provider);
        }

        if (!data.isEmpty()) {
            final TaskProvider<AccessTransformerFileGenerator> generator = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), namePreFix + "AccessTransformerGenerator"), AccessTransformerFileGenerator.class, task -> {
                task.getOutput().set(new File(workspaceDirectory, "accesstransformers/" + namePreFix + "/_script-access-transformer.cfg"));
                task.getAdditionalTransformers().set(data);
            });
            dependentTaskConfigurationHandler.accept(generator);
            
            fileProducingTasks.add(generator);
        }

        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), String.format("apply%sAccessTransformer", StringCapitalizationUtils.capitalize(namePreFix))), AccessTransformer.class, task -> {
            for (TaskProvider<? extends WithOutput> fileRemapTask : fileProducingTasks) {
                task.getTransformers().from(fileRemapTask.flatMap(WithOutput::getOutput));
                task.dependsOn(fileRemapTask);
            }
        });
    }
}
