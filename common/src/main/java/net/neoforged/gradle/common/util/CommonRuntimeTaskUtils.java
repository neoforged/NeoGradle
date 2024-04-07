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
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public final class CommonRuntimeTaskUtils {

    private CommonRuntimeTaskUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: CommonRuntimeTaskUtils. This is a utility class");
    }

    public static TaskProvider<? extends AccessTransformer> createAccessTransformer(Definition<?> definition, String namePreFix, File workspaceDirectory, Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler, FileTree files, Collection<String> data) {
        final Collection<TaskProvider<? extends WithOutput>> fileProducingTasks = new ArrayList<>();
        final Map<String, Integer> indexes = new HashMap<>();

        files.visit(new EmptyFileVisitor() {
            @Override
            public void visitFile(@NotNull FileVisitDetails fileDetails) {
                indexes.compute(fileDetails.getName(), (s, index) -> index == null ? 0 : index + 1);
                final int index = indexes.get(fileDetails.getName());

                final String name = CommonRuntimeUtils.buildTaskName(definition.getSpecification(), namePreFix + "AccessTransformerProvider" + fileDetails.getName() + (index == 0 ? "" : "_" + index));

                final TaskProvider<? extends WithOutput> provider = definition.getSpecification().getProject().getTasks().register(name, ArtifactProvider.class, task -> {
                    task.getInputFiles().from(
                            files.matching(f -> f.include(fileDetails.getPath()))
                    );
                    String outputFileName = fileDetails.getName();
                    if (index > 0) {
                        int extensionDot = outputFileName.lastIndexOf('.');
                        if (extensionDot == -1) {
                            outputFileName += "_" + index;
                        } else {
                            outputFileName = outputFileName.substring(0, extensionDot) + "_" + index + outputFileName.substring(extensionDot);
                        }
                    }
                    task.getOutputFileName().set(outputFileName);
                    task.getOutput().set(new File(workspaceDirectory, "accesstransformers/" + namePreFix + "/" + outputFileName));
                });

                fileProducingTasks.add(provider);
            }
        });

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
