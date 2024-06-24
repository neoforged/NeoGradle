package net.neoforged.gradle.common.util;

import net.neoforged.gradle.common.runtime.tasks.BinaryAccessTransformer;
import net.neoforged.gradle.common.runtime.tasks.SourceAccessTransformer;
import net.neoforged.gradle.common.runtime.tasks.AccessTransformerFileGenerator;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

public final class CommonRuntimeTaskUtils {

    private CommonRuntimeTaskUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: CommonRuntimeTaskUtils. This is a utility class");
    }

    public static TaskProvider<? extends SourceAccessTransformer> createSourceAccessTransformer(Definition<?> definition, String namePreFix, File workspaceDirectory, Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler, FileTree files, Collection<String> data, TaskProvider<? extends WithOutput> listLibs, FileCollection additionalClasspathElements) {
        final TaskProvider<AccessTransformerFileGenerator> generator;
        if (!data.isEmpty()) {
            generator = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), namePreFix + "AccessTransformerGenerator"), AccessTransformerFileGenerator.class, task -> {
                task.getOutput().set(new File(workspaceDirectory, "accesstransformers/" + namePreFix + "/_script-access-transformer.cfg"));
                task.getAdditionalTransformers().set(data);
            });
            dependentTaskConfigurationHandler.accept(generator);
        } else {
            generator = null;
        }

        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), String.format("apply%sAccessTransformer", StringCapitalizationUtils.capitalize(namePreFix))), SourceAccessTransformer.class, task -> {
            task.getTransformers().from(files);
            if (generator != null) {
                task.getTransformers().from(generator.flatMap(WithOutput::getOutput));
                task.dependsOn(generator);
            }
            task.dependsOn(listLibs);
            task.getLibraries().set(listLibs.flatMap(WithOutput::getOutput));
            task.getClasspath().from(additionalClasspathElements);
        });
    }

    public static TaskProvider<? extends BinaryAccessTransformer> createBinaryAccessTransformer(Definition<?> definition, String namePreFix, File workspaceDirectory, Consumer<TaskProvider<? extends Runtime>> dependentTaskConfigurationHandler, FileTree files, Collection<String> data) {
        final TaskProvider<AccessTransformerFileGenerator> generator;
        if (!data.isEmpty()) {
            generator = definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), namePreFix + "AccessTransformerGenerator"), AccessTransformerFileGenerator.class, task -> {
                task.getOutput().set(new File(workspaceDirectory, "accesstransformers/" + namePreFix + "/_script-access-transformer.cfg"));
                task.getAdditionalTransformers().set(data);
            });
            dependentTaskConfigurationHandler.accept(generator);
        } else {
            generator = null;
        }

        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), String.format("apply%sAccessTransformer", StringCapitalizationUtils.capitalize(namePreFix))), BinaryAccessTransformer.class, task -> {
            task.getTransformers().from(files);
            if (generator != null) {
                task.getTransformers().from(generator.flatMap(WithOutput::getOutput));
                task.dependsOn(generator);
            }
        });
    }
}
