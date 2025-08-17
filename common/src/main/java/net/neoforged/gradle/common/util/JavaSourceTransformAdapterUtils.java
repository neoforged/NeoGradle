package net.neoforged.gradle.common.util;

import com.google.common.collect.Maps;
import net.neoforged.gradle.common.extensions.subsystems.SubsystemsExtension;
import net.neoforged.gradle.common.runtime.tasks.JavaSourceTransformer;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import net.neoforged.gradle.dsl.common.extensions.InterfaceInjections;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.Nullable;

import static net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension.configureCommonRuntimeTaskParameters;

public class JavaSourceTransformAdapterUtils
{

    private JavaSourceTransformAdapterUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: McpAccessTransformerUtils. This is a utility class");
    }

    public static TaskTreeAdapter createCustomizationsAdapter(final Project project, final FileCollection systemAccessTransformers) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);
        final AccessTransformers accessTransformers = minecraftExtension.getAccessTransformers();
        final InterfaceInjections interfaceInjections = minecraftExtension.getInterfaceInjections();
        final SubsystemsExtension.ParchmentExtensions parchment = (SubsystemsExtension.ParchmentExtensions) project.getExtensions().getByType(Subsystems.class).getParchment();

        return (definition, previousTasksOutput, runtimeWorkspace, gameArtifacts, mappingVersionData, dependentTaskConfigurationHandler) -> {
            final FileCollection accessTransformerFiles = systemAccessTransformers.plus(accessTransformers.getFiles());

            var transformer = createJavaSourceTransformerTask(accessTransformerFiles,
                interfaceInjections.getFiles(),
                definition,
                previousTasksOutput,
                parchment);

            if (transformer != null)
            {
                definition.additionalCompileSources(transformer.flatMap(JavaSourceTransformer::getStubs));
            }

            return transformer;
        };
    }

    public static @Nullable TaskProvider<JavaSourceTransformer> createJavaSourceTransformerTask(
        final FileCollection accessTransformerFiles,
        final FileCollection interfaceInjectionFiles,
        final Definition<?> definition,
        final Provider<? extends WithOutput> previousTasksOutput,
        final SubsystemsExtension.ParchmentExtensions parchment)
    {
        final Provider<String> parchmentArtifact = parchment.getSelectedParchmentArtifact(definition.getSpecification()
            .getMinecraftVersion());

        if (accessTransformerFiles.isEmpty() && interfaceInjectionFiles.isEmpty() && !parchmentArtifact.isPresent()) {
            return null;
        }

        return definition.getSpecification()
            .getProject()
            .getTasks()
            .register(CommonRuntimeUtils.buildTaskName(definition.getSpecification(), "transformSource"), JavaSourceTransformer.class, task -> {
                task.getTransformers().from(accessTransformerFiles);
                task.getInterfaceInjections().from(interfaceInjectionFiles);

                if (parchmentArtifact.isPresent())
                {
                    task.getParchmentMappings().from(
                        ToolUtilities.resolveConfigurationFor(definition.getSpecification().getProject(), parchmentArtifact.get())
                    );
                    task.getParchmentConflictPrefix().set(parchment.getConflictPrefix());
                }

                task.getInputFile().set(previousTasksOutput.flatMap(WithOutput::getOutput));
                task.dependsOn(definition.getListLibrariesTaskProvider());
                task.getLibraries().set(definition.getListLibrariesTaskProvider().flatMap(WithOutput::getOutput));
                task.getClasspath().from(definition.getAllDependencies());
            });
    }
}
