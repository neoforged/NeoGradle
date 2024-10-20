package net.neoforged.gradle.neoform.util;

import net.neoforged.gradle.common.runtime.tasks.SourceAccessTransformer;
import net.neoforged.gradle.common.runtime.tasks.SourceInterfaceInjection;
import net.neoforged.gradle.common.util.CommonRuntimeTaskUtils;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import net.neoforged.gradle.dsl.common.extensions.InterfaceInjections;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

public class NeoFormAccessTaskAdapterUtils {

    private NeoFormAccessTaskAdapterUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: McpAccessTransformerUtils. This is a utility class");
    }

    public static TaskTreeAdapter createAccessTransformerAdapter(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);
        final AccessTransformers accessTransformerFiles = minecraftExtension.getAccessTransformers();

        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            if (accessTransformerFiles.getFiles().isEmpty()) {
                return null;
            }

            final TaskProvider<? extends SourceAccessTransformer> accessTransformerTask = CommonRuntimeTaskUtils.createSourceAccessTransformer(definition, "User", accessTransformerFiles.getFiles().getAsFileTree(), definition.getListLibrariesTaskProvider(), definition.getAllDependencies());
            accessTransformerTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            accessTransformerTask.configure(task -> task.dependsOn(previousTasksOutput));
            return accessTransformerTask;
        };
    }

    public static TaskTreeAdapter createInterfaceInjectionAdapter(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);
        final InterfaceInjections interfaceInjectionFiles = minecraftExtension.getInterfaceInjections();

        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            if (interfaceInjectionFiles.getFiles().isEmpty()) {
                return null;
            }

            final TaskProvider<? extends SourceInterfaceInjection> interfaceInjectionTask = CommonRuntimeTaskUtils.createSourceInterfaceInjection(definition, "User", interfaceInjectionFiles.getFiles().getAsFileTree(), definition.getListLibrariesTaskProvider(), definition.getAllDependencies());
            interfaceInjectionTask.configure(task -> task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput)));
            interfaceInjectionTask.configure(task -> task.dependsOn(previousTasksOutput));

            //Register the stubs
            definition.additionalCompileSources(
                    project.zipTree(
                            interfaceInjectionTask.flatMap(SourceInterfaceInjection::getStubs)
                    )
            );

            return interfaceInjectionTask;
        };
    }
}
