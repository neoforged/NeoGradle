package net.neoforged.gradle.common.util;

import net.neoforged.gradle.common.runtime.tasks.*;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.TaskProvider;

public final class CommonRuntimeTaskUtils {

    private CommonRuntimeTaskUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: CommonRuntimeTaskUtils. This is a utility class");
    }

    public static TaskProvider<? extends SourceAccessTransformer> createSourceAccessTransformer(Definition<?> definition, String namePreFix, FileTree files, TaskProvider<? extends WithOutput> listLibs, FileCollection additionalClasspathElements) {
        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), String.format("apply%sAccessTransformer", StringCapitalizationUtils.capitalize(namePreFix))), SourceAccessTransformer.class, task -> {
            task.getTransformers().from(files);
            task.dependsOn(listLibs);
            task.getLibraries().set(listLibs.flatMap(WithOutput::getOutput));
            task.getClasspath().from(additionalClasspathElements);
        });
    }

    public static TaskProvider<? extends SourceInterfaceInjection> createSourceInterfaceInjection(Definition<?> definition, String namePreFix, FileTree files, TaskProvider<? extends WithOutput> listLibs, FileCollection additionalClasspathElements) {
        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), String.format("apply%sInterfaceInjection", StringCapitalizationUtils.capitalize(namePreFix))), SourceInterfaceInjection.class, task -> {
            task.getTransformers().from(files);
            task.dependsOn(listLibs);
            task.getLibraries().set(listLibs.flatMap(WithOutput::getOutput));
            task.getClasspath().from(additionalClasspathElements);
        });
    }

    public static TaskProvider<? extends BinaryAccessTransformer> createBinaryAccessTransformer(Definition<?> definition, String namePreFix, FileTree files) {
        return definition.getSpecification().getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), String.format("apply%sAccessTransformer", StringCapitalizationUtils.capitalize(namePreFix))), BinaryAccessTransformer.class, task -> {
            task.getTransformers().from(files);
        });
    }
}
