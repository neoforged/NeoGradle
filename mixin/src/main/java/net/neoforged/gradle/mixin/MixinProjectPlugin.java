package net.neoforged.gradle.mixin;

import net.minecraftforge.srgutils.IMappingFile;
import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.RuntimesExtension;
import net.neoforged.gradle.common.tasks.WriteIMappingsFile;
import net.neoforged.gradle.common.util.CacheableIMappingFile;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.Runs;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.mixin.extension.Mixin;
import net.neoforged.gradle.util.IMappingFileUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MixinProjectPlugin implements Plugin<Project> {

    public static final String REFMAP_REMAP_MAPPINGS_TASK_NAME = "writeMappingsForMixinRefmapRemapping";
    public static final String AP_MAPPINGS_TASK_NAME = "writeMappingsForMixinAP";
    public static final String REF_MAP_FILE_PROP_KEY = "refMapFile";
    public static final String TSRG_FILE_PROP_KEY = "tsrgFile";
    public static final String REF_MAP_PROP_KEY = "refMap";

    private Project project;
    private Mixin extension;
    private String version;

    @Override
    public void apply(Project project) {
        if (project.getPlugins().findPlugin(CommonProjectPlugin.class) == null) {
            throw new IllegalStateException("The mixin extension requires the common plugin to be applied first.");
        }
        this.project = project;
        this.extension = project.getExtensions().create(Mixin.class, Mixin.EXTENSION_NAME, MixinExtension.class, project);
        this.version = determinePluginVersion();
        this.project.afterEvaluate(p -> {
            final ExtensionContainer extensions = this.project.getExtensions();
            extensions.getByType(RuntimesExtension.class).getAllDefinitions().forEach(this::createRefmapMappingsTask);
            // todo refmap and extra mappings
            extensions.getByType(SourceSetContainer.class).all(this::configureSourceSet);
            this.project.getTasks().withType(Jar.class).all(this::configureJarTask);
            extensions.getByType(Runs.class).all(this::configureRun);
        });
    }

    private String determinePluginVersion() {
        return this.project
                .getBuildscript()
                .getConfigurations()
                .getByName("classpath")
                .getResolvedConfiguration()
                .getFirstLevelModuleDependencies()
                .stream()
                .filter(dependency -> dependency.getModuleGroup().equals("net.neoforged.gradle") && dependency.getModuleName().equals("mixin"))
                .findFirst()
                .map(ResolvedDependency::getModuleVersion)
                .orElseThrow(() -> new IllegalStateException("Plugin could not determine its own version"));
    }

    private void configureSourceSet(SourceSet sourceSet) {
        final ExtraPropertiesExtension extraProperties = sourceSet.getExtensions().getExtraProperties();
        if (!extraProperties.has(REF_MAP_PROP_KEY)) return;
        final Object refMapFileName = extraProperties.get(REF_MAP_PROP_KEY);
        if (refMapFileName == null) return;

        final TaskProvider<JavaCompile> compileTaskProvider = this.project.getTasks().named(sourceSet.getCompileJavaTaskName(), JavaCompile.class);

        compileTaskProvider.configure(compile -> {
            final File destinationDirectory = compile.getTemporaryDir();
            final File refMapFile = new RefMapFile(destinationDirectory, "refmap.json", (String) refMapFileName);
            final File tsrgFile = new File(destinationDirectory, "mappings.tsrg");
            compile.getExtensions().getExtraProperties().set(REF_MAP_FILE_PROP_KEY, refMapFile);
            compile.getExtensions().getExtraProperties().set(TSRG_FILE_PROP_KEY, tsrgFile);
            compile.getOutputs().file(refMapFile);
            compile.getOutputs().file(tsrgFile);
            final Map<String, Object> apArgs = computeApArgs();
            compile.getInputs().properties(apArgs);
            final Provider<RegularFile> generatedMappingsFile = this.project.provider(() -> TaskDependencyUtils.extractRuntimeDefinition(this.project, sourceSet).getTask(AP_MAPPINGS_TASK_NAME)).map(TransformerUtils.guard(mappingTask -> {
                compile.dependsOn(mappingTask);
                return mappingTask.get().getOutput().get();
            }));
            final RegularFile reobfTsrgFile = this.extension.getReobfTsrgFile().orElse(generatedMappingsFile).get();
            compile.getInputs().file(reobfTsrgFile);
            apArgs.put("reobfTsrgFile", reobfTsrgFile.getAsFile().getAbsolutePath());
            apArgs.put("outRefMapFile", refMapFile.getAbsolutePath());
            apArgs.put("outTsrgFile", tsrgFile.getAbsolutePath());
            compile.getOptions().getCompilerArgs().addAll(apArgs.entrySet().stream().filter(e -> e.getValue() != null).map(e -> "-A" + e.getKey() + "=" + e.getValue()).collect(Collectors.toList()));
        });
    }

    @NotNull
    private Map<String, Object> computeApArgs() {
        final Map<String, Object> apArgs = new HashMap<>();
        apArgs.put("mappingTypes", "tsrg");
        apArgs.put("pluginVersion", this.version.contains("neo") ? this.version : this.version + "-neo");
        apArgs.put("disableTargetValidator", this.extension.getDisableTargetValidator().getOrNull());
        apArgs.put("disableTargetExport", this.extension.getDisableTargetExport().getOrNull());
        apArgs.put("disableOverwriteChecker", this.extension.getDisableOverwriteChecker().getOrNull());
        apArgs.put("quiet", this.extension.getQuiet().getOrNull());
        apArgs.put("showMessageTypes", this.extension.getShowMessageTypes().getOrNull());
        // todo compiler args
        return apArgs;
    }

    private void configureJarTask(Jar jar) {
        jar.getManifest().getAttributes().computeIfAbsent("MixinConfigs", $ -> String.join(",", extension.getConfigs().get()));
        for (Task task : jar.getSource().getBuildDependencies().getDependencies(jar)) {
            if (!(task instanceof JavaCompile)) continue;
            final ExtensionContainer extensions = task.getExtensions();
            final ExtraPropertiesExtension props = extensions.getExtraProperties();
            if (!props.has(REF_MAP_FILE_PROP_KEY)) continue;
            final RefMapFile refMapFile = (RefMapFile) props.get(REF_MAP_FILE_PROP_KEY);
            if (refMapFile == null) continue;
            if (!refMapFile.exists()) continue;
            jar.from(refMapFile, copy -> {
                copy.rename(s -> refMapFile.fileInJar.getName());
                copy.into(refMapFile.fileInJar.getParent() != null ? refMapFile.fileInJar.getParent() : "/");
            });
        }
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

    private void createRefmapMappingsTask(CommonRuntimeDefinition<?> runtimeDefinition) {
        runtimeDefinition.getTasks().computeIfAbsent(CommonRuntimeUtils.buildTaskName(runtimeDefinition, REFMAP_REMAP_MAPPINGS_TASK_NAME), name -> {
            final TaskProvider<WriteIMappingsFile> writeMappingsForMixinRefmapRemapping = this.project.getTasks().register(name, WriteIMappingsFile.class, task -> {
                final TaskProvider<? extends WithOutput> mappingsTaskProvider = runtimeDefinition.getRuntimeToSourceMappingsTaskProvider();
                final Provider<IMappingFile> mappings = mappingsTaskProvider.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile).map(TransformerUtils.guard(IMappingFileUtils::load));
                task.getMappings().set(mappings.map(CacheableIMappingFile::new));
                task.getFormat().set(IMappingFile.Format.SRG);
                task.dependsOn(mappingsTaskProvider);
            });
            runtimeDefinition.configureAssociatedTask(writeMappingsForMixinRefmapRemapping);
            return writeMappingsForMixinRefmapRemapping;
        });
        runtimeDefinition.getTasks().computeIfAbsent(CommonRuntimeUtils.buildTaskName(runtimeDefinition, AP_MAPPINGS_TASK_NAME), name -> {
            final TaskProvider<WriteIMappingsFile> writeMappingsForMixinRefmapRemapping = this.project.getTasks().register(name, WriteIMappingsFile.class, task -> {
                final TaskProvider<? extends WithOutput> mappingsTaskProvider = runtimeDefinition.getRuntimeToSourceMappingsTaskProvider();
                final Provider<IMappingFile> mappings = mappingsTaskProvider.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile).map(TransformerUtils.guard(IMappingFileUtils::load));
                task.getMappings().set(mappings.map(IMappingFile::reverse).map(CacheableIMappingFile::new));
                task.dependsOn(mappingsTaskProvider);
            });
            runtimeDefinition.configureAssociatedTask(writeMappingsForMixinRefmapRemapping);
            return writeMappingsForMixinRefmapRemapping;
        });
    }

    private static class RefMapFile extends File {
        private final File fileInJar;
        public RefMapFile(File directory, String fileName, String refMapFile) {
            super(directory, fileName);
            fileInJar = new File(refMapFile);
        }
    }
}
