package net.neoforged.gradle.mixin;

import net.minecraftforge.srgutils.IMappingFile;
import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.tasks.WriteIMappingsFile;
import net.neoforged.gradle.common.util.CacheableIMappingFile;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.Runs;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.dsl.mixin.extension.Mixin;
import net.neoforged.gradle.util.GradleInternalUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;

public class MixinProjectPlugin implements Plugin<Project> {

    public static final String REFMAP_REMAP_MAPPINGS_TASK_NAME = "writeMappingsForMixinRefmapRemapping";

    private Project project;
    private Mixin extension;

    @Override
    public void apply(Project project) {
        if (project.getPlugins().findPlugin(CommonProjectPlugin.class) == null) {
            throw new IllegalStateException("The mixin extension requires the common plugin to be applied first.");
        }
        this.project = project;
        this.extension = project.getExtensions().create(Mixin.class, Mixin.EXTENSION_NAME, MixinExtension.class, project);
        this.project.afterEvaluate(p -> {
            expandRuntimeDefinitions();
            p.getTasks().withType(Jar.class).configureEach(this::configureJarTask);
            // todo refmap and extra mappings
            // todo validate AP
            // todo compiler args
            this.project.getExtensions().getByType(Runs.class).configureEach(this::configureRun);
        });
    }

    private void configureJarTask(Jar jar) {
        jar.getManifest().getAttributes().computeIfAbsent("MixinConfigs", $ -> String.join(",", extension.getConfigs().get()));
    }

    private void configureRun(Run run) {
        final CommonRuntimeDefinition<?> runtimeDefinition = getRuntimeDefinition(this.project, run);
        final TaskProvider<? extends WithOutput> refmapMappingsTask = runtimeDefinition.getTask(REFMAP_REMAP_MAPPINGS_TASK_NAME);
        final MapProperty<String, String> systemProperties = run.getSystemProperties();
        final ListProperty<String> programArguments = run.getProgramArguments();
        systemProperties.put("mixin.env.refMapRemappingFile", refmapMappingsTask.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile).map(File::getAbsolutePath));
        systemProperties.put("mixin.env.remapRefMap", "true");
        for (String config : this.extension.getConfigs().get()) {
            programArguments.addAll("--mixin.config", config);
        }
        run.dependsOn(refmapMappingsTask);
    }

    private void expandRuntimeDefinitions() {
        GradleInternalUtils.getExtensions(this.project.getExtensions())
                .stream()
                .filter(CommonRuntimeExtension.class::isInstance)
                .map(extension -> (CommonRuntimeExtension<?,?,? extends CommonRuntimeDefinition<?>>) extension)
                .flatMap(ext -> ext.getRuntimes().get().values().stream())
                .forEach(runtimeDefinition -> createRefmapMappingsTask(this.project, runtimeDefinition));
    }

    private static CommonRuntimeDefinition<?> getRuntimeDefinition(Project project, Run run) {
        if (run.getExtensions().getExtraProperties().has("runtimeDefinition")) {
            return (CommonRuntimeDefinition<?>) run.getExtensions().getExtraProperties().get("runtimeDefinition");
        }
        try {
            return TaskDependencyUtils.extractRuntimeDefinition(project, run.getModSources().get());
        } catch (MultipleDefinitionsFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createRefmapMappingsTask(Project project, CommonRuntimeDefinition<?> runtimeDefinition) {
        runtimeDefinition.getTasks().computeIfAbsent(CommonRuntimeUtils.buildTaskName(runtimeDefinition, REFMAP_REMAP_MAPPINGS_TASK_NAME), name -> {
            final TaskProvider<WriteIMappingsFile> writeMappingsForMixinRefmapRemapping = project.getTasks().register(name, WriteIMappingsFile.class, task -> {
                final TaskProvider<? extends WithOutput> clientMappingsTask = runtimeDefinition.getGameArtifactProvidingTasks().get(GameArtifact.CLIENT_MAPPINGS);
                final TaskProvider<? extends WithOutput> mergedMappingsTask = runtimeDefinition.getTask("mergeMappings");
                final Provider<IMappingFile> clientMappings = clientMappingsTask.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile).map(TransformerUtils.guard(IMappingFile::load));
                final Provider<IMappingFile> mergedMappings = mergedMappingsTask.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile).map(TransformerUtils.guard(IMappingFile::load));
                final Provider<IMappingFile> reversedClientMappings = clientMappings.map(IMappingFile::reverse);
                final Provider<IMappingFile> reversedMergedMappings = mergedMappings.map(IMappingFile::reverse);
                final Provider<IMappingFile> mappings = reversedMergedMappings.zip(reversedClientMappings, IMappingFile::chain);
                task.getMappings().set(mappings.map(CacheableIMappingFile::new));
                task.getFormat().set(IMappingFile.Format.SRG);
                task.dependsOn(clientMappingsTask, mergedMappingsTask);
            });
            runtimeDefinition.configureAssociatedTask(writeMappingsForMixinRefmapRemapping);
            return writeMappingsForMixinRefmapRemapping;
        });
    }
}
