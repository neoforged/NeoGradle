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
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public final class CommonRuntimeTaskUtils {

    private CommonRuntimeTaskUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: CommonRuntimeTaskUtils. This is a utility class");
    }

    public static TaskProvider<? extends AccessTransformer> createAccessTransformer(Definition<?> definition, String namePreFix, File workspaceDirectory, Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler, Iterable<File> files, Collection<String> data, TaskProvider<? extends WithOutput> listLibs) {
        final Collection<TaskProvider<? extends WithOutput>> fileProducingTasks = new ArrayList<>();
        final Map<String, Integer> indexes = new HashMap<>();
        for (File file : files) {
            indexes.compute(file.getName(), (s, index) -> index == null ? 0 : index + 1);
            final int index = indexes.get(file.getName());
            
            final String name = CommonRuntimeUtils.buildTaskName(definition.getSpecification(), namePreFix + "AccessTransformerProvider" + file.getName() + (index == 0 ? "" : "_" + index));
            
            final TaskProvider<? extends WithOutput> provider = definition.getSpecification().getProject().getTasks().register(name, ArtifactProvider.class, task -> {
                task.getInput().set(file);
                String outputFileName = file.getName();
                if (index > 0) {
                    int extensionDot = outputFileName.lastIndexOf('.');
                    if (extensionDot == -1) {
                        outputFileName += "_" + index;
                    } else {
                        outputFileName = outputFileName.substring(0, extensionDot) + "_" + index + outputFileName.substring(extensionDot);
                    }
                }
                task.getOutput().set(new File(workspaceDirectory, "accesstransformers/" + namePreFix + "/" + outputFileName));
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
            task.dependsOn(listLibs);
            task.getLibraries().set(listLibs.flatMap(WithOutput::getOutput));
        });
    }
}
