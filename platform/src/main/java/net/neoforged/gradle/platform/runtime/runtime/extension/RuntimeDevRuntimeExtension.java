package net.neoforged.gradle.platform.runtime.runtime.extension;

import io.codechicken.diffpatch.util.PatchMode;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.runtime.tasks.DefaultExecute;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.specifications.ExecuteSpecification;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.platform.runtime.runtime.definition.RuntimeDevRuntimeDefinition;
import net.neoforged.gradle.platform.runtime.runtime.specification.RuntimeDevRuntimeSpecification;
import net.neoforged.gradle.platform.runtime.runtime.tasks.ApplyPatches;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class RuntimeDevRuntimeExtension extends CommonRuntimeExtension<RuntimeDevRuntimeSpecification, RuntimeDevRuntimeSpecification.Builder, RuntimeDevRuntimeDefinition> {
    
    @Inject
    public RuntimeDevRuntimeExtension(Project project) {
        super(project);
    }
    
    @Override
    protected @NotNull RuntimeDevRuntimeDefinition doCreate(RuntimeDevRuntimeSpecification spec) {
        final File workingDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("platform/%s", spec.getIdentifier())).get().getAsFile();

        NeoFormRuntimeDefinition neoformRuntime = spec.getNeoFormRuntime();

        final TaskProvider<? extends WithOutput> patchBase;
        if (spec.getParchmentArtifact() != null) {
            patchBase = applyParchment(
                    spec.getProject(),
                    "applyParchment",
                    getProject().provider(() -> ToolUtilities.resolveTool(getProject(), spec.getParchmentArtifact())),
                    getProject().provider(() -> "p_"),
                    neoformRuntime.getSourceJarTask().flatMap(WithOutput::getOutput).map(RegularFile::getAsFile),
                    true,
                    spec,
                    workingDirectory,
                    null
            );
        } else {
            patchBase = neoformRuntime.getSourceJarTask();
        }

        final TaskProvider<ApplyPatches> patchApply = spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "applyPatches"), ApplyPatches.class, task -> {
            task.getBase().set(patchBase.flatMap(WithOutput::getOutput));
            task.getPatches().set(spec.getPatchesDirectory());
            task.getRejects().set(spec.getRejectsDirectory());
            task.getPatchMode().set(spec.isUpdating() ? PatchMode.FUZZY : PatchMode.ACCESS);
            task.getShouldFailOnPatchFailure().set(!spec.isUpdating());
            configureCommonRuntimeTaskParameters(task, "applyPatches", spec, workingDirectory);
        });

        final TaskProvider<ArtifactProvider> sourcesProvider = spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "sourceFromAppliedPatches"), ArtifactProvider.class, task -> {
            task.getInputFiles().from(patchApply.flatMap(WithOutput::getOutput));
            task.getOutput().set(new File(workingDirectory, "patched.jar"));
        });
        
        return new RuntimeDevRuntimeDefinition(
                spec,
                neoformRuntime,
                sourcesProvider,
                patchBase);
    }
    
    @Override
    protected RuntimeDevRuntimeSpecification.Builder createBuilder() {
        return RuntimeDevRuntimeSpecification.Builder.from(getProject());
    }

    public static TaskProvider<DefaultExecute> applyParchment(Project project,
                                                       String name,
                                                       Provider<File> mappingsFile,
                                                       Provider<String> conflictPrefix,
                                                       Provider<File> input,
                                                       boolean inputFile,
                                                       CommonRuntimeSpecification spec,
                                                       File workingDirectory,
                                                       @Nullable TaskProvider<? extends WithOutput> extraClasspath) {
        return project.getTasks().register(CommonRuntimeUtils.buildTaskName(spec, name), DefaultExecute.class, task -> {
            File toolExecutable = ToolUtilities.resolveTool(project, project.getExtensions().getByType(Subsystems.class).getTools().getJST().get());

            task.getArguments().putFile("mappings", mappingsFile);
            if (inputFile) {
                task.getArguments().putFile("input", input);
            } else {
                task.getArguments().putDirectoryFile("input", input);
            }

            task.getExecutingJar().set(toolExecutable);
            task.getProgramArguments().add("--enable-parchment");
            task.getProgramArguments().add("--no-parchment-javadoc");
            task.getProgramArguments().add("--parchment-mappings");
            task.getProgramArguments().add("{mappings}");
            task.getProgramArguments().add("--in-format=" + (inputFile ? "archive" : "folder"));
            task.getProgramArguments().add("--out-format=archive");
            if (conflictPrefix.isPresent() && !conflictPrefix.get().isBlank()) {
                task.getProgramArguments().add("--parchment-conflict-prefix=%s".formatted(conflictPrefix.get()));
            }
            final StringBuilder builder = new StringBuilder();
            project.getExtensions().getByType(SourceSetContainer.class).getByName("main").getCompileClasspath().forEach(f -> {
                if (!builder.isEmpty()) {
                    builder.append(File.pathSeparator);
                }
                builder.append(f.getAbsolutePath());
            });
            if (extraClasspath != null) {
                builder.append(File.pathSeparator).append(extraClasspath.get().getOutput().get().getAsFile().getAbsolutePath());
                task.dependsOn(extraClasspath);
            }
            task.getProgramArguments().add("--classpath=" + builder);

            task.getLogLevel().set(ExecuteSpecification.LogLevel.DISABLED);
            task.getProgramArguments().add("{input}");
            task.getProgramArguments().add("{output}");

            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, name, spec, workingDirectory);
        });
    }
}
