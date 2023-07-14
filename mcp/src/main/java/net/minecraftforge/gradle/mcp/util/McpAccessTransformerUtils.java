package net.minecraftforge.gradle.mcp.util;

import net.minecraftforge.gradle.common.runtime.tasks.AccessTransformer;
import net.minecraftforge.gradle.common.util.CommonRuntimeTaskUtils;
import net.minecraftforge.gradle.dsl.common.extensions.AccessTransformers;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import java.util.ArrayList;

public class McpAccessTransformerUtils {

    private McpAccessTransformerUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: McpAccessTransformerUtils. This is a utility class");
    }

    public static TaskTreeAdapter createAccessTransformerAdapter(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);
        final AccessTransformers accessTransformerFiles = minecraftExtension.getAccessTransformers();

        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            if (accessTransformerFiles.getFiles().isEmpty() && accessTransformerFiles.getEntries().get().isEmpty()) {
                return null;
            }

            final TaskProvider<? extends AccessTransformer> accessTransformerTask = CommonRuntimeTaskUtils.createAccessTransformer(definition, "User", runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler, new ArrayList<>(accessTransformerFiles.getFiles().getFiles()), accessTransformerFiles.getEntries().get());
            accessTransformerTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            accessTransformerTask.configure(task -> task.dependsOn(previousTasksOutput));
            return accessTransformerTask;
        };
    }
}
