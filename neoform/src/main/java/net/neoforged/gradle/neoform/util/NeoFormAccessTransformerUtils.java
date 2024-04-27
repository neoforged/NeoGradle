package net.neoforged.gradle.neoform.util;

import net.neoforged.gradle.common.runtime.tasks.AccessTransformer;
import net.neoforged.gradle.common.util.CommonRuntimeTaskUtils;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class NeoFormAccessTransformerUtils {

    private NeoFormAccessTransformerUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: McpAccessTransformerUtils. This is a utility class");
    }

    public static TaskTreeAdapter createAccessTransformerAdapter(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);
        final AccessTransformers accessTransformerFiles = minecraftExtension.getAccessTransformers();

        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            if (accessTransformerFiles.getFiles().isEmpty() && accessTransformerFiles.getEntries().get().isEmpty()) {
                return null;
            }

            final TaskProvider<? extends AccessTransformer> accessTransformerTask = CommonRuntimeTaskUtils.createAccessTransformer(definition, "User", runtimeWorkspace, dependentTaskConfigurationHandler, accessTransformerFiles.getFiles().getAsFileTree(), accessTransformerFiles.getEntries().get());
            accessTransformerTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            accessTransformerTask.configure(task -> task.dependsOn(previousTasksOutput));
            return accessTransformerTask;
        };
    }
}
