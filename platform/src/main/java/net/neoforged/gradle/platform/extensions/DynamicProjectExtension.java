package net.neoforged.gradle.platform.extensions;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraftforge.gdi.BaseDSLElement;
import net.minecraftforge.gdi.annotations.DSLProperty;
import net.minecraftforge.gdi.annotations.ProjectGetter;
import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.dsl.common.runs.ide.extensions.IdeaRunExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.type.Type;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.dsl.platform.model.*;
import net.neoforged.gradle.neoform.NeoFormProjectPlugin;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.neoform.runtime.tasks.Download;
import net.neoforged.gradle.neoform.runtime.tasks.UnpackZip;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import net.neoforged.gradle.platform.PlatformDevProjectPlugin;
import net.neoforged.gradle.platform.model.DynamicProjectType;
import net.neoforged.gradle.platform.runtime.runtime.definition.RuntimeDevRuntimeDefinition;
import net.neoforged.gradle.platform.runtime.runtime.extension.RuntimeDevRuntimeExtension;
import net.neoforged.gradle.platform.runtime.runtime.tasks.GenerateBinaryPatches;
import net.neoforged.gradle.platform.runtime.runtime.tasks.GenerateSourcePatches;
import net.neoforged.gradle.platform.runtime.runtime.tasks.PackZip;
import net.neoforged.gradle.platform.tasks.*;
import net.neoforged.gradle.platform.util.ArtifactPathsCollector;
import net.neoforged.gradle.platform.util.SetupUtils;
import net.neoforged.gradle.util.HashFunction;
import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.vanilla.VanillaProjectPlugin;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.neoforged.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.apache.tools.ant.filters.ReplaceTokens;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.neoforged.gradle.dsl.common.util.Artifact.from;

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
    
    public void runtime(final String neoFormVersion) {
        runtime(
                neoFormVersion,
                project.getRootProject().getLayout().getProjectDirectory().dir("patches"),
                project.getRootProject().getLayout().getProjectDirectory().dir("rejects")
        );
    }
    
    public void runtime(final String neoFormVersion, Directory patches, Directory rejects) {
        type = DynamicProjectType.RUNTIME;
        
        project.getPlugins().apply(PlatformDevProjectPlugin.class);
        
        final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
        
        final RuntimeDevRuntimeExtension runtimeDevRuntimeExtension = project.getExtensions().getByType(RuntimeDevRuntimeExtension.class);
        final RuntimeDevRuntimeDefinition runtimeDefinition = runtimeDevRuntimeExtension.create(builder -> builder.withNeoFormVersion(neoFormVersion)
                                                                                                                     .withPatchesDirectory(patches)
                                                                                                                     .withRejectsDirectory(rejects)
                                                                                                                     .withDistributionType(DistributionType.JOINED)
                                                                                                                     .isUpdating(getIsUpdating()));
        
        final EnumMap<DistributionType, TaskProvider<? extends WithOutput>> neoformRawJarProviders = new EnumMap<>(DistributionType.class);
        neoformRawJarProviders.put(DistributionType.CLIENT, runtimeDefinition.getClientNeoFormRuntimeDefinition().getRawJarTask());
        neoformRawJarProviders.put(DistributionType.SERVER, runtimeDefinition.getServerNeoFormRuntimeDefinition().getRawJarTask());
        neoformRawJarProviders.put(DistributionType.JOINED, runtimeDefinition.getJoinedNeoFormRuntimeDefinition().getRawJarTask());
        
        final TaskProvider<? extends WithOutput> neoFormSources = runtimeDefinition.getJoinedNeoFormRuntimeDefinition().getSourceJarTask();
        
        final TaskProvider<SetupProjectFromRuntime> setupTask = configureSetupTasks(runtimeDefinition.getSourceJarTask().flatMap(WithOutput::getOutput), mainSource, runtimeDefinition.getMinecraftDependenciesConfiguration());
        setupTask.configure(task -> task.getShouldLockDirectories().set(false));
        
        final File workingDirectory = getProject().getLayout().getBuildDirectory().dir(String.format("platformplatform/%s", runtimeDefinition.getSpecification().getIdentifier())).get().getAsFile();
        
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
        for (DistributionType distribution : DistributionType.values()) {
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
        final Configuration serverExtraConfiguration = project.getConfigurations().create("serverExtra");
        final Configuration installerConfiguration = project.getConfigurations().create("installer");
        final Configuration installerLibrariesConfiguration = project.getConfigurations().create("installerLibraries");
        final Configuration moduleOnlyConfiguration = project.getConfigurations().create("moduleOnly").setTransitive(false);
        final Configuration gameLayerLibraryConfiguration = project.getConfigurations().create("gameLayerLibrary").setTransitive(false);
        final Configuration pluginLayerLibraryConfiguration = project.getConfigurations().create("pluginLayerLibrary").setTransitive(false);
        
        clientExtraConfiguration.getDependencies().add(project.getDependencies().create(
                ExtraJarDependencyManager.generateClientCoordinateFor(runtimeDefinition.getSpecification().getMinecraftVersion()))
        );
        
        serverExtraConfiguration.getDependencies().add(project.getDependencies().create(
                ExtraJarDependencyManager.generateServerCoordinateFor(runtimeDefinition.getSpecification().getMinecraftVersion()))
        );
        
        installerLibrariesConfiguration.extendsFrom(installerConfiguration);
        installerConfiguration.getDependencies().add(runtimeDefinition.getJoinedNeoFormRuntimeDefinition().getSpecification().getNeoFormArtifact().toDependency(project));
        
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
        
        final LauncherProfile launcherProfile = project.getExtensions().create(LauncherProfile.class, "launcherProfile", LauncherProfile.class);
        launcherProfile.configure((Action<LauncherProfile>) profile -> {
            profile.getId().set(String.format("%s-%s", project.getName(), project.getVersion()));
            profile.getTime().set(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            profile.getReleaseTime().set(profile.getTime());
            profile.getType().set("release");
            profile.getMainClass().set("cpw.mods.bootstraplauncher.BootstrapLauncher");
            profile.getInheritsFrom().set(runtimeDefinition.getSpecification().getMinecraftVersion());
            
            //TODO: Deal with logging when model for it stands
            profile.getLoggingConfiguration().set(project.getObjects().newInstance(LauncherProfile.LoggingConfiguration.class));
            
            final LauncherProfile.Arguments arguments = project.getObjects().newInstance(LauncherProfile.Arguments.class);
            arguments.game("--launchtarget");
            arguments.game("forgeclient");
            
            arguments.jvm("-Djava.net.preferIPv6Addresses=system");
            arguments.jvm(createIgnoreList(moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, project).map(ignoreList -> "-DignoreList=" + ignoreList + ",${version_name}.jar"));
            arguments.jvm("-DmergeModules=jna-5.10.0.jar,jna-platform-5.10.0.jar");
            arguments.jvm(collectFileNames(pluginLayerLibraryConfiguration, project).map(pluginLayerLibraries -> "-Dfml.pluginLayerLibraries=" + pluginLayerLibraries));
            arguments.jvm(collectFileNames(gameLayerLibraryConfiguration, project).map(gameLayerLibraries -> "-Dfml.gameLayerLibraries=" + gameLayerLibraries));
            arguments.jvm("-DlibraryDirectory=${library_directory}");
            arguments.jvm("-p");
            
            arguments.jvm(collectFilePaths(moduleOnlyConfiguration, "${library_directory}/", "${classpath_separator}", project));
            
            arguments.jvm("--add-modules");
            arguments.jvm("ALL-MODULE-PATH");
            arguments.jvm("--add-opens");
            arguments.jvm("java.base/java.util.jar=cpw.mods.securejarhandler");
            arguments.jvm("--add-opens");
            arguments.jvm("java.base/java.lang.invoke=cpw.mods.securejarhandler");
            arguments.jvm("--add-exports");
            arguments.jvm("java.base/sun.security.util=cpw.mods.securejarhandler");

            launcherProfile.getArguments().set(arguments);
        });
        
        final TaskProvider<CreateLauncherJson> createLauncherJson = project.getTasks().register("createLauncherJson", CreateLauncherJson.class, task -> {
            task.getProfile().set(launcherProfile);
            task.getLibraries().from(installerConfiguration);
            
            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
        
        final TaskProvider<? extends WithOutput> joinedCleanProvider = neoformRawJarProviders.get(DistributionType.JOINED);
        final TaskProvider<StripBinPatchedClasses> strippedJar = project.getTasks().register("stripBinaryPatchedClasses", StripBinPatchedClasses.class, task -> {
            task.getCompiled().set(project.getTasks().named(mainSource.getJarTaskName(), Jar.class).flatMap(Jar::getArchiveFile));
            task.getClean().set(joinedCleanProvider.flatMap(WithOutput::getOutput));
            
            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
        
        final TaskProvider<Jar> universalJar = project.getTasks().register("universalJar", Jar.class, task -> {
            task.getArchiveClassifier().set("universal-unsigned");
            task.getArchiveAppendix().set("universal-unsigned");
            task.getArchiveVersion().set(project.getVersion().toString());
            task.getArchiveBaseName().set(project.getName());
            task.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("libs"));
            task.getArchiveFileName().set(project.provider(() -> String.format("%s-%s-universal-unsigned.jar", project.getName(), project.getVersion())));
            
            task.dependsOn(strippedJar);
            
            task.from(project.zipTree(strippedJar.flatMap(WithOutput::getOutput)));
        });
        
        final TaskProvider<PotentiallySignJar> signUniversalJar = project.getTasks().register("signUniversalJar", PotentiallySignJar.class, task -> {
            task.getInput().set(universalJar.flatMap(Jar::getArchiveFile));
            task.getOutputFileName().set(project.provider(() -> String.format("%s-%s-universal.jar", project.getName(), project.getVersion())));
            
            task.dependsOn(universalJar);
        });
        
        final InstallerProfile installerProfile = project.getExtensions().create("installerProfile", InstallerProfile.class);
        installerProfile.configure((Consumer<InstallerProfile>) profile -> {
            profile.getProfile().set(project.getName());
            profile.getVersion().set(launcherProfile.getId());
            profile.getMinecraft().set(runtimeDefinition.getSpecification().getMinecraftVersion());
            profile.getServerJarPath().set("{LIBRARY_DIR}/net/minecraft/server/{MINECRAFT_VERSION}/server-{MINECRAFT_VERSION}.jar");
            profile.data(
                    "MAPPINGS",
                    String.format("[net.neoforged:neoform:%s:mappings@txt]", neoFormVersion),
                    String.format("[net.neoforged:neoform:%s:mappings@txt]", neoFormVersion)
            );
            profile.data(
                    "MOJMAPS",
                    String.format("[net.minecraft:client:%s:mappings@txt]", neoFormVersion),
                    String.format("[net.minecraft:server:%s:mappings@txt]", neoFormVersion)
            );
            profile.data(
                    "MERGED_MAPPINGS",
                    String.format("[net.neoforged:neoform:%s:mappings-merged@txt]", neoFormVersion),
                    String.format("[net.neoforged:neoform:%s:mappings-merged@txt]", neoFormVersion)
            );
            profile.data(
                    "BINPATCH",
                    "/data/client.lzma",
                    "/data/server.lzma"
            );
            profile.data(
                    "MC_UNPACKED",
                    String.format("[net.minecraft:client:%s:unpacked]", neoFormVersion),
                    String.format("[net.minecraft:server:%s:unpacked]", neoFormVersion)
            );
            profile.data(
                    "MC_SLIM",
                    String.format("[net.minecraft:client:%s:slim]", neoFormVersion),
                    String.format("[net.minecraft:server:%s:slim]", neoFormVersion)
            );
            profile.data(
                    "MC_EXTRA",
                    String.format("[net.minecraft:client:%s:client-extra]", neoFormVersion),
                    String.format("[net.minecraft:server:%s:server-extra]", neoFormVersion)
            );
            profile.data(
                    "MC_SRG",
                    String.format("[net.minecraft:client:%s:srg]", neoFormVersion),
                    String.format("[net.minecraft:server:%s:srg]", neoFormVersion)
            );
            profile.data(
                    "PATCHED",
                    String.format("[%s:%s:%s:client]", project.getGroup(), project.getName(), project.getVersion()),
                    String.format("[%s:%s:%s:server]", project.getGroup(), project.getName(), project.getVersion())
            );
            profile.data(
                    "MCP_VERSION",
                    String.format("'%s'", neoFormVersion),
                    String.format("'%s'", neoFormVersion)
            );
            profile.processor(project, processor -> {
                processor.server();
                processor.getJar().set("net.minecraftforge:installertools:1.3.0");
                processor.arguments(
                        "--task", "EXTRACT_FILES",
                        "--archive", "{INSTALLER}",
                        
                        "--from", "data/run.sh",
                        "--to",   "{ROOT}/run.sh",
                        "--exec", "{ROOT}/run.sh",
                        
                        "--from", "data/run.bat",
                        "--to",   "{ROOT}/run.bat",
                        
                        "--from",     "data/user_jvm_args.txt",
                        "--to",       "{ROOT}/user_jvm_args.txt",
                        "--optional", "{ROOT}/user_jvm_args.txt",
                        
                        "--from", "data/win_args.txt",
                        "--to", String.format("{ROOT}/libraries/%s/%s/%s/win_args.txt", project.getGroup().toString().replaceAll("\\.", "/"), project.getName(), project.getVersion()),
                        
                        "--from", "data/unix_args.txt",
                        "--to", String.format("{ROOT}/libraries/%s/%s/%s/unix_args.txt", project.getGroup().toString().replaceAll("\\.", "/"), project.getName(), project.getVersion())
                );
            });
            profile.processor(project, processor -> {
                processor.server();
                processor.getJar().set("net.minecraftforge:installertools:1.3.0");
                processor.arguments(
                        "--task", "BUNDLER_EXTRACT",
                        "--input", "{MINECRAFT_JAR}",
                        "--output", "{ROOT}/libraries/",
                        "--libraries"
                );
            });
            profile.processor(project, processor -> {
                processor.server();
                processor.getJar().set("net.minecraftforge:installertools:1.3.0");
                processor.arguments(
                        "--task", "BUNDLER_EXTRACT",
                        "--input", "{MINECRAFT_JAR}",
                        "--output", "{MC_UNPACKED}",
                        "--jar-only"
                );
            });
            profile.processor(project, processor -> {
                processor.getJar().set("net.minecraftforge:installertools:1.3.0");
                processor.arguments(
                        "--task", "MCP_DATA",
                        "--input", String.format("[%s]", runtimeDefinition.getJoinedNeoFormRuntimeDefinition().getSpecification().getNeoFormArtifact().toString()),
                        "--output", "{MAPPINGS}",
                        "--key", "mappings"
                );
            });
            profile.processor(project, processor -> {
                processor.getJar().set("net.minecraftforge:installertools:1.3.0");
                processor.arguments(
                        "--task", "DOWNLOAD_MOJMAPS",
                        "--version", runtimeDefinition.getSpecification().getMinecraftVersion(),
                        "--side", "{SIDE}",
                        "--output", "{MOJMAPS}"
                );
            });
            profile.processor(project, processor -> {
                processor.getJar().set("net.minecraftforge:installertools:1.3.0");
                processor.arguments(
                        "--task", "MERGE_MAPPING",
                        "--left", "{MAPPINGS}",
                        "--right", "{MOJMAPS}",
                        "--output", "{MERGED_MAPPINGS}",
                        "--classes", "--fields", "--methods", "--reverse-right"
                );
            });
            profile.processor(project, processor -> {
                processor.client();
                processor.getJar().set("net.minecraftforge:jarsplitter:1.1.4");
                processor.arguments(
                        "--input", "{MINECRAFT_JAR}",
                        "--slim",  "{MC_SLIM}",
                        "--extra", "{MC_EXTRA}",
                        "--srg", "{MERGED_MAPPINGS}"
                );
            });
            profile.processor(project, processor -> {
                processor.server();
                processor.getJar().set("net.minecraftforge:jarsplitter:1.1.4");
                processor.arguments(
                        "--input", "{MC_UNPACKED}",
                        "--slim",  "{MC_SLIM}",
                        "--extra", "{MC_EXTRA}",
                        "--srg", "{MERGED_MAPPINGS}"
                );
            });
            profile.processor(project, processor -> {
                processor.getJar().set("net.minecraftforge:ForgeAutoRenamingTool:0.1.22:all");
                processor.arguments(
                        "--input", "{MC_SLIM}",
                        "--output", "{MC_SRG}",
                        "--names", "{MERGED_MAPPINGS}",
                        "--ann-fix", "--ids-fix", "--src-fix", "--record-fix"
                );
            });
            profile.processor(project, processor -> {
                processor.getJar().set("net.minecraftforge:binarypatcher:1.1.1");
                processor.arguments(
                        "--clean", "{MC_SRG}",
                        "--output", "{PATCHED}",
                        "--apply", "{BINPATCH}"
                );
            });
            
            profile.getLibraries().add(Library.fromOutput(signUniversalJar, project, "universal"));
            
            profile.getIcon().set(project.provider(() -> {
                final File icon = new File(project.getRootProject().getProjectDir(), "docs/assets/neoforged.ico");
                if (!icon.exists()) {
                    throw new IllegalStateException("Missing icon.");
                }
                return "data:image/png;base64," + new String(Base64.getEncoder().encode(Files.readAllBytes(icon.toPath())));
            }));
            profile.getJson().set("/version.json");
            profile.getLogo().set("/big_logo.png");
            profile.getMirrorList().set("https://mirrors.neoforged.net");
            profile.getWelcome().set("Welcome to the simple installer for " + project.getName());
        });
        
        final TaskProvider<CreateLegacyInstallerJson> createLegacyInstallerJson = project.getTasks().register("createLegacyInstallerJson", CreateLegacyInstallerJson.class, task -> {
            task.getProfile().set(installerProfile);
            task.getLibraries().from(installerLibrariesConfiguration);
            
            task.dependsOn(signUniversalJar);
            
            clientExtraConfiguration.getDependencies().getBuildDependencies().getDependencies(null).forEach(task::dependsOn);
            serverExtraConfiguration.getDependencies().getBuildDependencies().getDependencies(null).forEach(task::dependsOn);
            
            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
        
        final Configuration installerToolConfiguration = ConfigurationUtils.temporaryConfiguration(project, project.getDependencies().create("net.minecraftforge:installer:2.1.+:shrunk"));
        final TaskProvider<Download> downloadInstaller = project.getTasks().register("downloadInstaller", Download.class, task -> {
            task.getInput().from(installerToolConfiguration);
            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
        
        final TaskProvider<CreateClasspathFiles> createWindowsServerArgsFile = project.getTasks().register("createWindowsServerArgsFile", CreateClasspathFiles.class, task -> {
            task.getModulePath().from(moduleOnlyConfiguration);
            task.getClasspath().from(installerConfiguration);
            task.getClasspath().from(gameLayerLibraryConfiguration);
            task.getClasspath().from(pluginLayerLibraryConfiguration);
            task.getPathSeparator().set(";");
            task.getServer().set(runtimeDefinition.getGameArtifactProvidingTasks().get(GameArtifact.SERVER_JAR).flatMap(WithOutput::getOutput));
            task.getNeoFormVersion().set(neoFormVersion);
            
            configureInstallerTokens(task, runtimeDefinition, Lists.newArrayList(
                    moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration
            ), pluginLayerLibraryConfiguration, gameLayerLibraryConfiguration);
            
            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
        
        final TaskProvider<CreateClasspathFiles> createUnixServerArgsFile = project.getTasks().register("createUnixServerArgsFile", CreateClasspathFiles.class, task -> {
            task.getModulePath().from(moduleOnlyConfiguration);
            task.getClasspath().from(installerConfiguration);
            task.getClasspath().from(gameLayerLibraryConfiguration);
            task.getClasspath().from(pluginLayerLibraryConfiguration);
            task.getPathSeparator().set(":");
            task.getServer().set(runtimeDefinition.getGameArtifactProvidingTasks().get(GameArtifact.SERVER_JAR).flatMap(WithOutput::getOutput));
            task.getNeoFormVersion().set(neoFormVersion);
            
            configureInstallerTokens(task, runtimeDefinition, Lists.newArrayList(
                    moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration
            ), pluginLayerLibraryConfiguration, gameLayerLibraryConfiguration);
            
            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
        
        final TaskProvider<CreateLegacyInstaller> installerJar = project.getTasks().register("legacyInstallerJar", CreateLegacyInstaller.class, task -> {
            task.getInstallerCore().set(downloadInstaller.flatMap(WithOutput::getOutput));
            task.getInstallerJson().set(createLegacyInstallerJson.flatMap(WithOutput::getOutput));
            task.getLauncherJson().set(createLauncherJson.flatMap(WithOutput::getOutput));
            task.getClientBinaryPatches().set(binaryPatchGenerators.get(DistributionType.CLIENT).flatMap(WithOutput::getOutput));
            task.getServerBinaryPatches().set(binaryPatchGenerators.get(DistributionType.SERVER).flatMap(WithOutput::getOutput));
            task.getWindowsServerArgs().set(createWindowsServerArgsFile.flatMap(WithOutput::getOutput));
            task.getUnixServerArgs().set(createUnixServerArgsFile.flatMap(WithOutput::getOutput));
            task.getData().from(project.getRootProject().fileTree("server_files/").exclude("args.txt"));
            
            configureInstallerTokens(task, runtimeDefinition, Lists.newArrayList(
                    moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration
            ), pluginLayerLibraryConfiguration, gameLayerLibraryConfiguration);
            
            if (project.getProperties().containsKey("neogradle.runtime.platform.installer.debug") && Boolean.parseBoolean(project.getProperties().get("neogradle.runtime.platform.installer.debug").toString())) {
                task.from(signUniversalJar.flatMap(WithOutput::getOutput), spec -> {
                    spec.into("/maven/" + getProject().getGroup().toString().replace(".", "/") + "/" + getProject().getName() + "/" + getProject().getVersion() + "/");
                });
            }
        });
        
        TaskProvider<PotentiallySignJar> signInstallerJar = project.getTasks().register("signInstallerJar", PotentiallySignJar.class, task -> {
            task.getInput().set(installerJar.flatMap(Zip::getArchiveFile));
            task.getOutputFileName().set(project.provider(() -> String.format("%s-%s-installer.jar", project.getName(), project.getVersion())));
            
            task.dependsOn(installerJar);
        });
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
    
    private void configureRunType(final Project project, final Type type, final Configuration moduleOnlyConfiguration, final Configuration gameLayerLibraryConfiguration, final Configuration pluginLayerLibraryConfiguration, RuntimeDevRuntimeDefinition runtimeDefinition) {
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName("main");
        
        final Configuration runtimeClasspath = project.getConfigurations().getByName(mainSourceSet.getRuntimeClasspathConfigurationName());
        
        type.getMainClass().set("cpw.mods.bootstraplauncher.BootstrapLauncher");
        
        type.getSystemProperties().put("java.net.preferIPv6Addresses", "system");
        type.getJvmArguments().addAll("-p", moduleOnlyConfiguration.getAsPath());
        
        type.getSystemProperties().put("ignoreList", createIgnoreList(moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, project));
        type.getSystemProperties().put("mergeModules", "jna-5.10.0.jar,jna-platform-5.10.0.jar");
        type.getSystemProperties().put("fml.pluginLayerLibraries", collectFileNames(pluginLayerLibraryConfiguration, project));
        type.getSystemProperties().put("fml.gameLayerLibraries", collectFileNames(gameLayerLibraryConfiguration, project));
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
    
    private static void configureInstallerTokens(final TokenizedTask tokenizedTask, final RuntimeDevRuntimeDefinition runtimeDefinition, final Collection<Configuration> ignoreConfigurations, final Configuration pluginLayerLibraries, final Configuration gameLayerLibraries) {
        tokenizedTask.token("TASK", "forgeserver");
        tokenizedTask.token("MAVEN_PATH", String.format("%s/%s/%s", tokenizedTask.getProject().getGroup().toString().replace('.', '/'), tokenizedTask.getProject().getName(), tokenizedTask.getProject().getVersion()));
        tokenizedTask.token("FORGE_VERSION", tokenizedTask.getProject().getVersion());
        tokenizedTask.token("FML_VERSION", tokenizedTask.getProject().getProperties().get("fancy_mod_loader_version"));
        tokenizedTask.token("MC_VERSION", runtimeDefinition.getSpecification().getMinecraftVersion());
        tokenizedTask.token("MCP_VERSION", runtimeDefinition.getJoinedNeoFormRuntimeDefinition().getSpecification().getNeoFormVersion());
        tokenizedTask.token("FORGE_GROUP", tokenizedTask.getProject().getGroup());
        tokenizedTask.token("IGNORE_LIST", ignoreConfigurations.stream().flatMap(config -> config.getFiles().stream()).map(file -> {
            if (file.getName().startsWith("events") || file.getName().startsWith("core")) {
                return file.getName();
            }
            return file.getName().replaceAll("([-_]([.\\d]*\\d+)|\\.jar$)", "");
        }).collect(Collectors.joining(",")));
        tokenizedTask.token("PLUGIN_LAYER_LIBRARIES", pluginLayerLibraries.getFiles().stream().map(File::getName).collect(Collectors.joining(",")));
        tokenizedTask.token("GAME_LAYER_LIBRARIES", gameLayerLibraries.getFiles().stream().map(File::getName).collect(Collectors.joining(",")));
        tokenizedTask.token("MODULES", "ALL-MODULE-PATH");
    }
    
    private static Provider<String> collectFileNames(Configuration config, Project project) {
        return project.provider(() -> config.getFiles().stream().map(File::getName).collect(Collectors.joining(",")));
    }
    
    @NotNull
    private static Provider<String> createIgnoreList(Configuration moduleOnlyConfiguration, Configuration gameLayerLibraryConfiguration, Configuration pluginLayerLibraryConfiguration, Project project) {
        return project.provider(() -> {
            StringBuilder ignoreList = new StringBuilder(1000);
            for (Configuration cfg : Arrays.asList(moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration)) {
                ignoreList.append(cfg.getFiles().stream().map(file -> (file.getName().startsWith("events") || file.getName().startsWith("core") ? file.getName() : file.getName().replaceAll("([-_]([.\\d]*\\d+)|\\.jar$)", ""))).collect(Collectors.joining(","))).append(",");
            }
            ignoreList.append("client-extra").append(",").append(project.getName()).append("-");
            return ignoreList.toString();
        });
    }
    
    private static Provider<String> collectFilePaths(Configuration config, String prefix, String seperator, Project project) {
        return project.provider(() -> {
            final ArtifactPathsCollector collector = new ArtifactPathsCollector(project.getObjects(), seperator, prefix);
            config.getAsFileTree().visit(collector);
            return collector.toString();
        });
    }
    
    private void configureRun(final Project project, final Run run, final RuntimeDevRuntimeDefinition runtimeDefinition) {
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
