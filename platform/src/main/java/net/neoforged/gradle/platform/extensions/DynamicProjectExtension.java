package net.neoforged.gradle.platform.extensions;

import net.minecraftforge.gdi.BaseDSLElement;
import net.minecraftforge.gdi.annotations.DSLProperty;
import net.minecraftforge.gdi.annotations.ProjectGetter;
import net.neoforged.gradle.common.dependency.ClientExtraJarDependencyManager;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.dsl.common.runs.ide.extensions.IdeaRunExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.type.Type;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.neoform.NeoFormProjectPlugin;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.neoform.runtime.tasks.UnpackZip;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import net.neoforged.gradle.platform.PlatformDevProjectPlugin;
import net.neoforged.gradle.platform.model.DynamicProjectType;
import net.neoforged.gradle.platform.runtime.definition.PlatformDevRuntimeDefinition;
import net.neoforged.gradle.platform.runtime.extension.PlatformDevRuntimeExtension;
import net.neoforged.gradle.platform.runtime.tasks.GenerateBinaryPatches;
import net.neoforged.gradle.platform.runtime.tasks.GenerateSourcePatches;
import net.neoforged.gradle.platform.runtime.tasks.PackZip;
import net.neoforged.gradle.platform.tasks.SetupProjectFromRuntime;
import net.neoforged.gradle.platform.util.SetupUtils;
import net.neoforged.gradle.vanilla.VanillaProjectPlugin;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.neoforged.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public abstract class DynamicProjectExtension implements BaseDSLElement<DynamicProjectExtension> {
    
    private final Project project;
    
    @Nullable
    private DynamicProjectType type = null;
    
    @Inject
    public DynamicProjectExtension(Project project) {
        this.project = project;
        this.getIsUpdating().convention(getProviderFactory().gradleProperty("updating").map(Boolean::valueOf).orElse(false));
    }
    
    @ProjectGetter
    @Override
    public Project getProject() {
        return project;
    }
    
    public void clean() {
        clean("+");
    }
    
    public void clean(final String minecraftVersion) {
        type = DynamicProjectType.CLEAN;
        
        project.getPlugins().apply(VanillaProjectPlugin.class);
        
        final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
        
        final VanillaRuntimeExtension vanillaRuntimeExtension = project.getExtensions().getByType(VanillaRuntimeExtension.class);
        final VanillaRuntimeDefinition runtimeDefinition = vanillaRuntimeExtension.create(builder -> builder.withMinecraftVersion(minecraftVersion)
                                                                                                             .withDistributionType(DistributionType.CLIENT)
                                                                                                             .withFartVersion(vanillaRuntimeExtension.getFartVersion())
                                                                                                             .withForgeFlowerVersion(vanillaRuntimeExtension.getVineFlowerVersion())
                                                                                                             .withAccessTransformerApplierVersion(vanillaRuntimeExtension.getAccessTransformerApplierVersion()));
        
        project.getTasks().named(mainSource.getCompileJavaTaskName()).configure(task -> task.setEnabled(false));
        
        configureSetupTasks(runtimeDefinition.getSourceJarTask().flatMap(WithOutput::getOutput), mainSource, runtimeDefinition.getMinecraftDependenciesConfiguration());
    }
    
    public void neoform() {
        //Accept any version of NeoForm. Aka the latest will always work.
        neoform("+");
    }
    
    public void neoform(final String neoFormVersion) {
        type = DynamicProjectType.NEO_FORM;
        
        project.getPlugins().apply(NeoFormProjectPlugin.class);
        
        final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
        
        final NeoFormRuntimeExtension neoFormRuntimeExtension = project.getExtensions().getByType(NeoFormRuntimeExtension.class);
        final NeoFormRuntimeDefinition runtimeDefinition = neoFormRuntimeExtension.create(builder -> {
            builder.withNeoFormVersion(neoFormVersion)
                    .withDistributionType(DistributionType.CLIENT);
            
            NeoFormRuntimeUtils.configureDefaultRuntimeSpecBuilder(project, builder);
        });
        
        configureSetupTasks(runtimeDefinition.getSourceJarTask().flatMap(WithOutput::getOutput), mainSource, runtimeDefinition.getMinecraftDependenciesConfiguration());
    }
    
    public void forge(final String neoFormVersion) {
        forge(
                neoFormVersion,
                project.getRootProject().getLayout().getProjectDirectory().dir("patches"),
                project.getRootProject().getLayout().getProjectDirectory().dir("rejects")
        );
    }
    
    public void forge(final String neoFormVersion, Directory patches, Directory rejects) {
        type = DynamicProjectType.FORGE;
        
        project.getPlugins().apply(PlatformDevProjectPlugin.class);
        
        final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
        
        final PlatformDevRuntimeExtension platformDevRuntimeExtension = project.getExtensions().getByType(PlatformDevRuntimeExtension.class);
        final PlatformDevRuntimeDefinition runtimeDefinition = platformDevRuntimeExtension.create(builder -> builder.withNeoFormVersion(neoFormVersion)
                                                                                                                     .withPatchesDirectory(patches)
                                                                                                                     .withRejectsDirectory(rejects)
                                                                                                                     .withDistributionType(DistributionType.JOINED)
                                                                                                                     .isUpdating(getIsUpdating()));
        
        final EnumMap<DistributionType, TaskProvider<? extends WithOutput>> neoformRawJarProviders = new EnumMap<>(DistributionType.class);
        neoformRawJarProviders.put(DistributionType.JOINED, runtimeDefinition.getJoinedNeoFormRuntimeDefinition().getRawJarTask());
        
        final TaskProvider<? extends WithOutput> neoFormSources = runtimeDefinition.getJoinedNeoFormRuntimeDefinition().getSourceJarTask();
        
        final TaskProvider<SetupProjectFromRuntime> setupTask = configureSetupTasks(runtimeDefinition.getSourceJarTask().flatMap(WithOutput::getOutput), mainSource, runtimeDefinition.getMinecraftDependenciesConfiguration());
        setupTask.configure(task -> task.getShouldLockDirectories().set(false));
        
        final File workingDirectory = getProject().getLayout().getBuildDirectory().dir(String.format("patchgeneration/%s", runtimeDefinition.getSpecification().getIdentifier())).get().getAsFile();
        
        final TaskProvider<? extends WithOutput> packChanges = project.getTasks().register("packForgeChanges", PackZip.class, task -> {
            task.getInputFiles().from(SetupUtils.getSetupSourceTarget(getProject()));
            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
        
        final TaskProvider<? extends GenerateSourcePatches> createPatches = project.getTasks().register("createSourcePatches", GenerateSourcePatches.class, task -> {
            task.getBase().set(neoFormSources.flatMap(WithOutput::getOutput));
            task.getModified().set(packChanges.flatMap(WithOutput::getOutput));
            
            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
        
        final TaskProvider<? extends UnpackZip> unpackZip = project.getTasks().register("unpackSourcePatches", UnpackZip.class, task -> {
            task.getInputZip().set(createPatches.flatMap(WithOutput::getOutput));
            task.getUnpackingTarget().set(patches);
            
            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
        
        final TaskProvider<? extends Jar> compiledJarProvider = project.getTasks().named(
                project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName("main").getJarTaskName(),
                Jar.class
        );
        
        final EnumMap<DistributionType, TaskProvider<GenerateBinaryPatches>> binaryPatchGenerators = new EnumMap<>(DistributionType.class);
        for (DistributionType distribution : neoformRawJarProviders.keySet()) {
            final TaskProvider<? extends WithOutput> cleanProvider = neoformRawJarProviders.get(distribution);
            final TaskProvider<GenerateBinaryPatches> generateBinaryPatchesTask = project.getTasks().register(distribution.createTaskName("generate", "BinaryPatches"), GenerateBinaryPatches.class, task -> {
                task.getClean().set(cleanProvider.flatMap(WithOutput::getOutput));
                task.getPatched().set(compiledJarProvider.flatMap(Jar::getArchiveFile));
                task.getDistributionType().set(distribution);
                
                task.mustRunAfter(unpackZip);
                task.mustRunAfter(setupTask);
                
                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
            });
            binaryPatchGenerators.put(distribution, generateBinaryPatchesTask);
        }
        
        final TaskProvider<?> generateBinaryPatches = project.getTasks().register("generateBinaryPatches", task -> {
            binaryPatchGenerators.values().forEach(task::dependsOn);
            task.setGroup("neogradle/runtime/platform");
        });
        
        final Configuration clientExtraConfiguration = project.getConfigurations().create("clientExtra");
        final Configuration installerConfiguration = project.getConfigurations().create("installer");
        final Configuration moduleOnlyConfiguration = project.getConfigurations().create("moduleOnly").setTransitive(false);
        final Configuration gameLayerLibraryConfiguration = project.getConfigurations().create("gameLayerLibrary").setTransitive(false);
        final Configuration pluginLayerLibraryConfiguration = project.getConfigurations().create("pluginLayerLibrary").setTransitive(false);
        
        clientExtraConfiguration.getDependencies().add(project.getDependencies().create(
                ClientExtraJarDependencyManager.generateCoordinateFor(runtimeDefinition.getSpecification().getMinecraftVersion()))
        );
        
        project.getConfigurations().getByName(mainSource.getImplementationConfigurationName()).extendsFrom(
                gameLayerLibraryConfiguration,
                pluginLayerLibraryConfiguration,
                installerConfiguration
        );
        project.getConfigurations().getByName(mainSource.getRuntimeClasspathConfigurationName()).extendsFrom(
                clientExtraConfiguration
        );
        
        project.getExtensions().configure(RunsConstants.Extensions.RUN_TYPES, (Action<NamedDomainObjectContainer<Type>>) types -> types.all(type -> configureRunType(project, type, moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, runtimeDefinition)));
        project.getExtensions().configure(RunsConstants.Extensions.RUNS, (Action<NamedDomainObjectContainer<Run>>) runs -> runs.all(run -> configureRun(project, run, runtimeDefinition)));
    }
    
    private TaskProvider<SetupProjectFromRuntime> configureSetupTasks(Provider<RegularFile> rawJarProvider, SourceSet mainSource, Configuration runtimeDefinition1) {
        final IdeManagementExtension ideManagementExtension = project.getExtensions().getByType(IdeManagementExtension.class);
        
        final TaskProvider<? extends Task> ideImportTask = ideManagementExtension.getOrCreateIdeImportTask();
        
        final TaskProvider<SetupProjectFromRuntime> projectSetup = project.getTasks().register("setup", SetupProjectFromRuntime.class, task -> {
            task.getSourcesFile().set(rawJarProvider);
            task.dependsOn(ideImportTask);
        });
        
        final Configuration implementation = project.getConfigurations().getByName(mainSource.getImplementationConfigurationName());
        runtimeDefinition1.getAllDependencies()
                .forEach(dep -> implementation.getDependencies().add(dep));
        
        final Project rootProject = project.getRootProject();
        if (!rootProject.getTasks().getNames().contains("setup")) {
            rootProject.getTasks().create("setup");
        }
        
        rootProject.getTasks().named("setup").configure(task -> task.dependsOn(projectSetup));
        
        return projectSetup;
    }
    
    private void configureRunType(final Project project, final Type type, final Configuration moduleOnlyConfiguration, final Configuration gameLayerLibraryConfiguration, final Configuration pluginLayerLibraryConfiguration, PlatformDevRuntimeDefinition runtimeDefinition) {
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName("main");
        
        final Configuration runtimeClasspath = project.getConfigurations().getByName(mainSourceSet.getRuntimeClasspathConfigurationName());
        
        type.getMainClass().set("cpw.mods.bootstraplauncher.BootstrapLauncher");
        
        type.getSystemProperties().put("java.net.preferIPv6Addresses", "system");
        type.getJvmArguments().addAll("-p", moduleOnlyConfiguration.getAsPath());
        
        StringBuilder ignoreList = new StringBuilder(1000);
        for (Configuration cfg : Arrays.asList(moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration)) {
            ignoreList.append(cfg.getFiles().stream().map(file -> (file.getName().startsWith("events") || file.getName().startsWith("core") ? file.getName() : file.getName().replaceAll("([-_]([.\\d]*\\d+)|\\.jar$)", ""))).collect(Collectors.joining(","))).append(",");
        }
        ignoreList.append("client-extra").append(",").append(project.getName()).append("-");
        type.getSystemProperties().put("ignoreList", ignoreList.toString());
        type.getSystemProperties().put("mergeModules", "jna-5.10.0.jar,jna-platform-5.10.0.jar");
        type.getSystemProperties().put("fml.pluginLayerLibraries", pluginLayerLibraryConfiguration.getFiles().stream().map(File::getName).collect(Collectors.joining(",")));
        type.getSystemProperties().put("fml.gameLayerLibraries", gameLayerLibraryConfiguration.getFiles().stream().map(File::getName).collect(Collectors.joining(",")));
        type.getSystemProperties().put("legacyClassPath", project.getConfigurations().getByName("runtimeClasspath").getAsPath());
        type.getJvmArguments().addAll("--add-modules", "ALL-MODULE-PATH");
        type.getJvmArguments().addAll("--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler");
        type.getJvmArguments().addAll("--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler");
        type.getJvmArguments().addAll("--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler");
        type.getJvmArguments().addAll("--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming");
        
        type.getEnvironmentVariables().put("FORGE_SPEC", project.getVersion().toString());
        
        type.getClasspath().from(runtimeClasspath);
        
        type.getRunAdapter().set(run -> {
            if (run.getIsClient().get()) {
                run.getProgramArguments().addAll("--username", "Dev");
                run.getProgramArguments().addAll("--version", project.getName());
                run.getProgramArguments().addAll("--accessToken", "0");
                run.getProgramArguments().addAll("--userrun", "mojang");
                run.getProgramArguments().addAll("--versionrun", "release");
                run.getProgramArguments().add("--assetsDir");
                run.getProgramArguments().add(runtimeDefinition.getAssetsTaskProvider().get().getOutputDirectory().get().getAsFile().getAbsolutePath());
                run.getProgramArguments().add("--assetIndex");
                run.getProgramArguments().add(runtimeDefinition.getAssetsTaskProvider().get().getAssetIndexFile().get().getAsFile().getName().substring(0, runtimeDefinition.getAssetsTaskProvider().get().getAssetIndexFile().get().getAsFile().getName().lastIndexOf('.')));
                run.getProgramArguments().addAll("--launchTarget", "forgeclientdev");
            }
            
            if (run.getIsServer().get()) {
                run.getProgramArguments().addAll("--launchTarget", "forgeserverdev");
            }
            
            if (run.getIsGameTest().get()) {
                run.getSystemProperties().put("forge.enableGameTest", "true");
            }
            
            if (run.getIsDataGenerator().get()) {
                run.getSystemProperties().put("--launchTarget", "forgedatadev");
                
                run.getProgramArguments().addAll(
                        "--flat",
                        "--all",
                        "--validate"
                );
                mainSourceSet.getResources().getSrcDirs().forEach(file -> {
                    run.getProgramArguments().addAll(
                            "--existing",
                            file.getAbsolutePath()
                    );
                });
            }
        });
    }
    
    
    private void configureRun(final Project project, final Run run, final PlatformDevRuntimeDefinition runtimeDefinition) {
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName("main");
        
        run.getConfigureAutomatically().set(true);
        run.getConfigureFromDependencies().set(false);
        run.getConfigureFromTypeWithName().set(true);
        
        run.getModSources().add(mainSourceSet);
        
        if (run.getIsClient().get()) {
            run.dependsOn(runtimeDefinition.getAssetsTaskProvider(), runtimeDefinition.getNativesTaskProvider());
        }
        
        project.getExtensions().getByType(IdeManagementExtension.class)
                .onIdea((project1, idea, ideaExtension) -> run.getExtensions().getByType(IdeaRunExtension.class).getPrimarySourceSet().convention(mainSourceSet));
        
        //TODO: Deal with the lazy component of this, we might in the future move all of this into the run definition.
        run.getEnvironmentVariables().put("MOD_CLASSES", Stream.concat(
                run.getModSources().get().stream().map(source -> source.getOutput().getResourcesDir()),
                run.getModSources().get().stream().map(source -> source.getOutput().getClassesDirs().getFiles()).flatMap(Collection::stream)
        ).map(File::getAbsolutePath).map(path -> String.format("minecraft%%%%%s", path)).collect(Collectors.joining(File.pathSeparator)));
    }
    
    @NotNull
    public DynamicProjectType getType() {
        if (type == null)
            throw new IllegalStateException("Project is not configured yet!");
        return type;
    }
    
    @DSLProperty
    public abstract Property<Boolean> getIsUpdating();
    
    @Inject
    public abstract ProviderFactory getProviderFactory();

}
