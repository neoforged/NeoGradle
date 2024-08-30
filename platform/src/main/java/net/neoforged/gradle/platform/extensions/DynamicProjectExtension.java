package net.neoforged.gradle.platform.extensions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import net.minecraftforge.gdi.BaseDSLElement;
import net.minecraftforge.gdi.annotations.DSLProperty;
import net.minecraftforge.gdi.annotations.ProjectGetter;
import net.minecraftforge.srgutils.IMappingFile;
import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.extensions.IdeManagementExtension;
import net.neoforged.gradle.common.extensions.JarJarExtension;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.tasks.AccessTransformerFileGenerator;
import net.neoforged.gradle.common.runtime.tasks.DefaultExecute;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.tasks.JarJar;
import net.neoforged.gradle.common.tasks.PotentiallySignJar;
import net.neoforged.gradle.common.tasks.WriteIMappingsFile;
import net.neoforged.gradle.common.util.CacheableIMappingFile;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.sourceset.SourceSetDependencyExtension;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.runs.type.RunTypeManager;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.*;
import net.neoforged.gradle.dsl.platform.dynamic.NeoFormProjectConfiguration;
import net.neoforged.gradle.dsl.platform.dynamic.RuntimeProjectConfiguration;
import net.neoforged.gradle.dsl.platform.model.InstallerProfile;
import net.neoforged.gradle.dsl.platform.model.LauncherProfile;
import net.neoforged.gradle.dsl.platform.model.Library;
import net.neoforged.gradle.dsl.platform.util.RepositoryCollection;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.neoform.NeoFormProjectPlugin;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.neoform.runtime.tasks.Download;
import net.neoforged.gradle.neoform.runtime.tasks.PackJar;
import net.neoforged.gradle.neoform.runtime.tasks.UnpackZip;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import net.neoforged.gradle.platform.PlatformDevProjectPlugin;
import net.neoforged.gradle.platform.model.DynamicProjectType;
import net.neoforged.gradle.platform.runtime.runtime.definition.RuntimeDevRuntimeDefinition;
import net.neoforged.gradle.platform.runtime.runtime.extension.RuntimeDevRuntimeExtension;
import net.neoforged.gradle.platform.runtime.runtime.specification.RuntimeDevRuntimeSpecification;
import net.neoforged.gradle.platform.runtime.runtime.tasks.GenerateBinaryPatches;
import net.neoforged.gradle.platform.runtime.runtime.tasks.GenerateSourcePatches;
import net.neoforged.gradle.platform.tasks.*;
import net.neoforged.gradle.platform.util.ArtifactPathsCollector;
import net.neoforged.gradle.platform.util.SetupUtils;
import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.vanilla.VanillaProjectPlugin;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import net.neoforged.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.FeatureSpec;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.neoforged.gradle.dsl.common.util.Constants.DEFAULT_PARCHMENT_ARTIFACT_PREFIX;
import static net.neoforged.gradle.dsl.common.util.Constants.DEFAULT_PARCHMENT_GROUP;

public abstract class DynamicProjectExtension implements BaseDSLElement<DynamicProjectExtension> {

    private final Project project;

    @Nullable
    private DynamicProjectType type = null;

    @Inject
    public DynamicProjectExtension(Project project) {
        this.project = project;
        this.getIsUpdating().convention(getProviderFactory().gradleProperty("updating").map(Boolean::valueOf).orElse(false));

        //All dynamic projects expose information from themselves as a library. Cause they are.
        project.getPlugins().apply("java-library");
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
        final VanillaRuntimeDefinition runtimeDefinition = vanillaRuntimeExtension.create(builder -> builder.withMinecraftVersion(minecraftVersion).withDistributionType(DistributionType.CLIENT).withFartVersion(vanillaRuntimeExtension.getFartVersion()).withForgeFlowerVersion(vanillaRuntimeExtension.getVineFlowerVersion()).withAccessTransformerApplierVersion(vanillaRuntimeExtension.getAccessTransformerApplierVersion()));

        project.getTasks().named(mainSource.getCompileJavaTaskName()).configure(task -> task.setEnabled(false));

        configureSetupTasks(runtimeDefinition.getSourceJarTask().flatMap(WithOutput::getOutput), mainSource, runtimeDefinition.getMinecraftDependenciesConfiguration());
    }

    public void neoform() {
        //Accept any version of NeoForm. Aka the latest will always work.
        neoform("+");
    }

    public void neoform(final String neoFormVersion) {
        neoform(neoFormVersion, null);
    }

    public void neoform(final String neoFormVersion, @Nullable final Action<NeoFormProjectConfiguration> configurator) {
        type = DynamicProjectType.NEO_FORM;

        final NeoFormProjectConfiguration configuration = project.getObjects().newInstance(NeoFormProjectConfiguration.class);
        configuration.getSplitSourceSets().convention(false);
        if (configurator != null) {
            configurator.execute(configuration);
        }

        final boolean splitSourceSets = configuration.getSplitSourceSets().get();

        project.getPlugins().apply(NeoFormProjectPlugin.class);

        final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
        final SourceSet clientSource = splitSourceSets ? javaPluginExtension.getSourceSets().maybeCreate("client") : mainSource;

        final NeoFormRuntimeExtension mainRuntimeExtension = project.getExtensions().getByType(NeoFormRuntimeExtension.class);
        final NeoFormRuntimeDefinition runtimeDefinition = mainRuntimeExtension.create(builder -> {
            builder.withNeoFormVersion(neoFormVersion).withDistributionType(
                    splitSourceSets ? DistributionType.SERVER : DistributionType.JOINED
            );

            NeoFormRuntimeUtils.configureDefaultRuntimeSpecBuilder(project, builder);
        });
        final NeoFormRuntimeDefinition clientRuntimeDefinition = splitSourceSets ? mainRuntimeExtension.create(builder -> {
            builder.withNeoFormVersion(neoFormVersion).withDistributionType(DistributionType.CLIENT).removeCommonElements();

            NeoFormRuntimeUtils.configureDefaultRuntimeSpecBuilder(project, builder);
        }) : runtimeDefinition;

        final var parchmentArtifact = getParchment().map(parch -> {
            var split = parch.split("-");
            return DEFAULT_PARCHMENT_GROUP
                    + ":" + DEFAULT_PARCHMENT_ARTIFACT_PREFIX + split[0]
                    + ":" + split[1]
                    + "@zip";
        });

        TaskProvider<? extends WithOutput> mainSourcesTask = runtimeDefinition.getSourceJarTask();
        TaskProvider<? extends WithOutput> clientSourcesTask = splitSourceSets ? clientRuntimeDefinition.getSourceJarTask() : mainSourcesTask;
        if (parchmentArtifact.isPresent()) {
            mainSourcesTask = RuntimeDevRuntimeExtension.applyParchment(
                    getProject(),
                    "applyParchment",
                    getProject().provider(() -> ToolUtilities.resolveTool(getProject(), parchmentArtifact.get())),
                    getProject().provider(() -> "p_"),
                    mainSourcesTask.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile),
                    true,
                    runtimeDefinition.getSpecification(),
                    project.getLayout().getBuildDirectory().dir(String.format("neoForm/%s", neoFormVersion)).get().getAsFile(),
                    null
            );

            if (splitSourceSets) {
                clientSourcesTask = RuntimeDevRuntimeExtension.applyParchment(
                        getProject(),
                        "applyParchment",
                        getProject().provider(() -> ToolUtilities.resolveTool(getProject(), parchmentArtifact.get())),
                        getProject().provider(() -> "p_"),
                        clientSourcesTask.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile),
                        true,
                        clientRuntimeDefinition.getSpecification(),
                        project.getLayout().getBuildDirectory().dir(String.format("neoForm/%s", neoFormVersion)).get().getAsFile(),
                        null
                );
            }
        }

        configureSetupTasks(mainSourcesTask.flatMap(WithOutput::getOutput), mainSource, runtimeDefinition.getMinecraftDependenciesConfiguration());
        if (splitSourceSets) {
            configureSetupTasks(clientSourcesTask.flatMap(WithOutput::getOutput), clientSource, clientRuntimeDefinition.getMinecraftDependenciesConfiguration());
        }
    }

    public void runtime(final String neoFormVersion) {
        runtime(neoFormVersion, project.getRootProject().getLayout().getProjectDirectory().dir("patches"), project.getRootProject().getLayout().getProjectDirectory().dir("rejects"));
    }

    public void runtime(final String neoFormVersion, Directory patches, Directory rejects) {
        runtime(runtimeProjectConfiguration -> {
            runtimeProjectConfiguration.getNeoFormVersion().set(neoFormVersion);
            runtimeProjectConfiguration.getPatches().set(patches);
            runtimeProjectConfiguration.getRejects().set(rejects);
        });
    }

    public void runtime(final Closure<Void> configureClosure) {
        this.runtime(configureClosure != null ? configureClosure::call : null);
    }

    public void runtime(@Nullable final Action<RuntimeProjectConfiguration> configurator) {
        type = DynamicProjectType.RUNTIME;

        final RuntimeProjectConfiguration configuration = project.getObjects().newInstance(RuntimeProjectConfiguration.class);
        configuration.getPatches().convention(project.getRootProject().getLayout().getProjectDirectory().dir("patches"));
        configuration.getRejects().convention(project.getRootProject().getLayout().getProjectDirectory().dir("rejects"));
        configuration.getSplitSourceSets().convention(false);
        if (configurator != null) {
            configurator.execute(configuration);
        }

        final String neoFormVersion = configuration.getNeoFormVersion().get();
        final boolean splitSourceSets = configuration.getSplitSourceSets().get();

        project.getPlugins().apply(PlatformDevProjectPlugin.class);

        final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);

        javaPluginExtension.withSourcesJar();
        javaPluginExtension.withJavadocJar();

        final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
        final SourceSet clientSource = splitSourceSets ? javaPluginExtension.getSourceSets().maybeCreate("client") : mainSource;

        if (splitSourceSets) {
            javaPluginExtension.registerFeature("client", featureSpec -> {
                featureSpec.usingSourceSet(clientSource);
                featureSpec.withJavadocJar();
                featureSpec.withSourcesJar();
            });
        }

        // Maven coordinates for the NeoForm version this runtime uses
        final NeoFormRuntimeExtension neoFormRuntimeExtension = getProject().getExtensions().getByType(NeoFormRuntimeExtension.class);
        final NeoFormRuntimeDefinition neoFormRuntimeDefinition = neoFormRuntimeExtension.create(builder -> {
            builder.withNeoFormVersion(neoFormVersion).withDistributionType(
                    splitSourceSets ? DistributionType.SERVER : DistributionType.JOINED
            );

            NeoFormRuntimeUtils.configureDefaultRuntimeSpecBuilder(project, builder);
        });
        final NeoFormRuntimeDefinition clientNeoFormRuntimeDefinition = splitSourceSets ? neoFormRuntimeExtension.create(builder -> {
            builder.withNeoFormVersion(neoFormVersion).withDistributionType(DistributionType.CLIENT).removeCommonElements();

            NeoFormRuntimeUtils.configureDefaultRuntimeSpecBuilder(project, builder);
        }) : neoFormRuntimeDefinition;
        final NeoFormRuntimeDefinition joinedRuntimeDefinition = !splitSourceSets ? neoFormRuntimeDefinition :
                neoFormRuntimeExtension.create(builder -> {
                    builder.withNeoFormVersion(neoFormVersion).withDistributionType(DistributionType.JOINED);

                    NeoFormRuntimeUtils.configureDefaultRuntimeSpecBuilder(project, builder);
                });

        // The NeoForm version that's passed into this method can be a version range or '+', but to build userdev and installer profiles safely,
        // we need the actual version it was resolved to. Otherwise, the NeoForm version used by installer & userdev could change over time.
        String neoFormDependency = "net.neoforged:neoform:" + neoFormRuntimeDefinition.getSpecification().getVersion() + "@zip";

        final var parchmentArtifact = getParchment().map(parch -> {
            var split = parch.split("-");
            return DEFAULT_PARCHMENT_GROUP
                    + ":" + DEFAULT_PARCHMENT_ARTIFACT_PREFIX + split[0]
                    + ":" + split[1]
                    + "@zip";
        });

        final Directory mainPatches = splitSourceSets ? project.getLayout().getProjectDirectory().dir("src/main/patches") : configuration.getPatches().get();
        final Directory clientPatches = splitSourceSets ? project.getLayout().getProjectDirectory().dir("src/client/patches") : mainPatches;

        final Directory mainRejects = splitSourceSets ? project.getLayout().getProjectDirectory().dir("rejects/main") : configuration.getRejects().get();
        final Directory clientRejects = splitSourceSets ? project.getLayout().getProjectDirectory().dir("rejects/client") : mainRejects;

        final RuntimeDevRuntimeExtension runtimeDevRuntimeExtension = project.getExtensions().getByType(RuntimeDevRuntimeExtension.class);
        final RuntimeDevRuntimeDefinition runtimeDefinition = runtimeDevRuntimeExtension.create(builder -> {
            builder.withNeoFormRuntime(neoFormRuntimeDefinition)
                    .withPatchesDirectory(mainPatches)
                    .withRejectsDirectory(mainRejects)
                    .withDistributionType(splitSourceSets ? DistributionType.SERVER : DistributionType.JOINED)
                    .withParchment(parchmentArtifact)
                    .isUpdating(getIsUpdating());
        });
        final RuntimeDevRuntimeDefinition clientRuntimeDefinition = splitSourceSets ? runtimeDevRuntimeExtension.create(builder -> {
            builder.withNeoFormRuntime(clientNeoFormRuntimeDefinition)
                    .withPatchesDirectory(clientPatches)
                    .withRejectsDirectory(clientRejects)
                    .withDistributionType(DistributionType.CLIENT)
                    .withParchment(parchmentArtifact)
                    .isUpdating(getIsUpdating());
        }) : runtimeDefinition;

        project.getExtensions().add("runtime", runtimeDefinition);
        project.getExtensions().add("mainRuntime", runtimeDefinition);
        if (splitSourceSets) {
            project.getExtensions().add("clientRuntime", clientRuntimeDefinition);
        }

        if (splitSourceSets) {
            final SourceSetDependencyExtension clientDependsOn = clientSource.getExtensions().getByType(SourceSetDependencyExtension.class);
            clientDependsOn.on(mainSource);
        }

        final IdeManagementExtension ideManagementExtension = project.getExtensions().getByType(IdeManagementExtension.class);
        if (splitSourceSets) {
            ideManagementExtension.registerTaskToRun(clientRuntimeDefinition.getAssets());
            ideManagementExtension.registerTaskToRun(clientRuntimeDefinition.getNatives());
        } else {
            ideManagementExtension.registerTaskToRun(runtimeDefinition.getAssets());
            ideManagementExtension.registerTaskToRun(runtimeDefinition.getNatives());
        }

        final File mainWorkingDirectory = getProject().getLayout().getBuildDirectory().dir(String.format("platform/%s", runtimeDefinition.getSpecification().getIdentifier())).get().getAsFile();
        //If split source sets are not enabled then this will be the same as mainWorkingDirectory
        final File clientWorkingDirectory = getProject().getLayout().getBuildDirectory().dir(String.format("platform/%s", clientRuntimeDefinition.getSpecification().getIdentifier())).get().getAsFile();

        final Configuration mergedModulesConfiguration = project.getConfigurations().create("mergedModules");
        final Configuration clientExtraConfiguration = project.getConfigurations().create("clientExtra");
        final Configuration serverExtraConfiguration = project.getConfigurations().create("serverExtra");
        final Configuration installerConfiguration = project.getConfigurations().create("installer");
        final Configuration installerLibrariesConfiguration = project.getConfigurations().create("installerLibraries");
        final Configuration moduleOnlyConfiguration = project.getConfigurations().create("moduleOnly").setTransitive(false);

        final Configuration gameLayerLibraryConfiguration = project.getConfigurations().create("gameLayerLibrary").setTransitive(false);
        final Configuration clientGameLayerLibraryConfiguration = splitSourceSets ? project.getConfigurations().create("clientGameLayerLibrary").setTransitive(false) : gameLayerLibraryConfiguration;

        final Configuration pluginLayerLibraryConfiguration = project.getConfigurations().create("pluginLayerLibrary").setTransitive(false);
        final Configuration clientPluginLayerLibraryConfiguration = splitSourceSets ? project.getConfigurations().create("clientPluginLayerLibrary").setTransitive(false) : pluginLayerLibraryConfiguration;

        final Configuration userdevCompileOnlyConfiguration = project.getConfigurations().create("userdevCompileOnly").setTransitive(false);
        final Configuration userdevTestImplementationConfiguration = project.getConfigurations().create("userdevTestImplementation").setTransitive(true);
        final Configuration jarJarConfiguration = project.getConfigurations().create("jarJar");

        if (splitSourceSets) {
            clientGameLayerLibraryConfiguration.extendsFrom(gameLayerLibraryConfiguration);
            clientPluginLayerLibraryConfiguration.extendsFrom(pluginLayerLibraryConfiguration);
        }

        clientExtraConfiguration.getDependencies().add(
                project.getDependencies().create(
                        ExtraJarDependencyManager.generateClientCoordinateFor(
                                clientRuntimeDefinition.getSpecification().getMinecraftVersion()
                        )));

        serverExtraConfiguration.getDependencies().add(
                project.getDependencies().create(
                        ExtraJarDependencyManager.generateServerCoordinateFor(
                                runtimeDefinition.getSpecification().getMinecraftVersion()
                        )));

        installerLibrariesConfiguration.extendsFrom(installerConfiguration);
        installerLibrariesConfiguration.getDependencies().add(project.getDependencyFactory().create(neoFormDependency));

        //TODO: Deal with the installer configuration, split? not split?
        project.getConfigurations().getByName(mainSource.getApiConfigurationName()).extendsFrom(gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, installerConfiguration);
        project.getConfigurations().getByName(clientSource.getApiConfigurationName()).extendsFrom(clientGameLayerLibraryConfiguration, clientPluginLayerLibraryConfiguration, installerConfiguration);

        project.getConfigurations().getByName(mainSource.getRuntimeClasspathConfigurationName()).extendsFrom(splitSourceSets ? serverExtraConfiguration : clientExtraConfiguration);
        if (splitSourceSets) {
            project.getConfigurations().getByName(clientSource.getRuntimeClasspathConfigurationName()).extendsFrom(clientExtraConfiguration);
        }

        project.getExtensions().configure(RunTypeManager.class, types ->
                types.configureEach(type ->
                        configureRunType(project, type, splitSourceSets, mergedModulesConfiguration, moduleOnlyConfiguration, clientGameLayerLibraryConfiguration, clientPluginLayerLibraryConfiguration
                        )));
        project.getExtensions().configure(RunManager.class, runs -> runs.configureAll(run -> configureRun(run, splitSourceSets, clientRuntimeDefinition)));

        project.getExtensions().create(net.neoforged.gradle.dsl.common.extensions.JarJar.class, JarJarExtension.EXTENSION_NAME, JarJarExtension.class, project);

        final LauncherProfile launcherProfile = project.getExtensions().create(LauncherProfile.class, "launcherProfile", LauncherProfile.class);
        final InstallerProfile installerProfile = project.getExtensions().create("installerProfile", InstallerProfile.class);
        final UserdevProfile userdevProfile = project.getExtensions().create("userdevProfile", UserdevProfile.class, project.getObjects());

        final TaskProvider<SetupProjectFromRuntime> setupTask = configureSetupTasks(runtimeDefinition.getSourceJarTask().flatMap(WithOutput::getOutput), mainSource, runtimeDefinition.getMinecraftDependenciesConfiguration());
        final TaskProvider<SetupProjectFromRuntime> clientSetupTask = splitSourceSets ? configureSetupTasks(clientRuntimeDefinition.getSourceJarTask().flatMap(WithOutput::getOutput), clientSource, clientRuntimeDefinition.getMinecraftDependenciesConfiguration()) : setupTask;
        setupTask.configure(task -> task.getShouldLockDirectories().set(false));
        clientSetupTask.configure(task -> task.getShouldLockDirectories().set(false));

        if (splitSourceSets && configuration.getLegacyPatches().isPresent()) {
            final Directory legacyPatches = configuration.getLegacyPatches().get();

            final TaskProvider<? extends WithOutput> serverMappings = runtimeDefinition.getGameArtifactProvidingTasks().get(GameArtifact.SERVER_MAPPINGS);
            final Provider<Set<String>> serverRelativePaths = serverMappings.flatMap(WithOutput::getOutput)
                            .map(RegularFile::getAsFile)
                            .map(File::toPath)
                            .map(FileUtils::readAllLines)
                            .map(lines -> lines
                                    .filter(line -> !line.startsWith("\t"))
                                    .filter(line -> !line.startsWith("#"))
                                    .map(line -> line.split(" ")[0])
                                    .map(line -> line.replace(".", "/"))
                                    .map(line -> line + ".java.patch")
                                    .collect(Collectors.toSet()));

            getProject().getTasks().register("migrateClientPatches", Copy.class, migrateTask -> {
                migrateTask.dependsOn(serverMappings);
                migrateTask.into(clientPatches);
                migrateTask.from(legacyPatches, copySpec -> {
                    copySpec.include(fileTreeElement -> {
                        if (fileTreeElement.isDirectory())
                            return true;

                        final String relativeFromRoot = fileTreeElement.getRelativePath().getPathString();
                        return !serverRelativePaths.get().contains(relativeFromRoot);
                    });
                    copySpec.setIncludeEmptyDirs(false);
                });

                migrateTask.getInputs().property("filter", serverRelativePaths);
            });

            getProject().getTasks().register("migrateMainPatches", Copy.class, migrateTask -> {
                migrateTask.dependsOn(serverMappings);
                migrateTask.into(mainPatches);
                migrateTask.from(legacyPatches, copySpec -> {
                    copySpec.include(fileTreeElement -> {
                        if (fileTreeElement.isDirectory())
                            return true;

                        final String relativeFromRoot = fileTreeElement.getRelativePath().getPathString();
                        return serverRelativePaths.get().contains(relativeFromRoot);
                    });
                    copySpec.setIncludeEmptyDirs(false);
                });

                migrateTask.getInputs().property("filter", serverRelativePaths);
            });
        }

        project.afterEvaluate(evaledProject -> {
            final EnumMap<DistributionType, TaskProvider<? extends WithOutput>> cleanProviders = new EnumMap<>(DistributionType.class);
            cleanProviders.put(DistributionType.CLIENT, createCleanProvider(clientRuntimeDefinition.getGameArtifactProvidingTasks().get(GameArtifact.CLIENT_JAR), clientRuntimeDefinition, clientWorkingDirectory));
            cleanProviders.put(DistributionType.SERVER, createCleanProvider(runtimeDefinition.getNeoFormRuntimeDefinition().getTask("extractServer"), runtimeDefinition, mainWorkingDirectory));
            cleanProviders.put(DistributionType.JOINED, joinedRuntimeDefinition.getTask("rename"));

            final EnumMap<DistributionType, TaskProvider<? extends WithOutput>> obfToMojMappingProviders = new EnumMap<>(DistributionType.class);
            final TaskProvider<? extends WithOutput> clientInverseMappings = createFlippedMojMapProvider(clientRuntimeDefinition.getGameArtifactProvidingTasks().get(GameArtifact.CLIENT_MAPPINGS), clientRuntimeDefinition, clientWorkingDirectory);
            final TaskProvider<? extends WithOutput> serverInverseMappings = createFlippedMojMapProvider(runtimeDefinition.getGameArtifactProvidingTasks().get(GameArtifact.SERVER_MAPPINGS), runtimeDefinition, mainWorkingDirectory);
            obfToMojMappingProviders.put(DistributionType.CLIENT, clientInverseMappings);
            obfToMojMappingProviders.put(DistributionType.SERVER, serverInverseMappings);
            obfToMojMappingProviders.put(DistributionType.JOINED, clientInverseMappings);

            final TaskProvider<? extends WithOutput> neoFormSources = runtimeDefinition.getNeoFormRuntimeDefinition().getSourceJarTask();
            final TaskProvider<? extends WithOutput> clientNeoFormSources = splitSourceSets ? clientRuntimeDefinition.getNeoFormRuntimeDefinition().getSourceJarTask() : neoFormSources;

            final TaskProvider<? extends WithOutput> packChanges = project.getTasks().register("packForgeChanges", PackJar.class, task -> {
                task.getInputFiles().from(SetupUtils.getSetupSourceTarget(mainSource));
                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, clientRuntimeDefinition, clientWorkingDirectory);
            });

            @Nullable TaskProvider<? extends WithOutput> clientPackChanges = splitSourceSets ? project.getTasks().register("packClientForgeChanges", PackJar.class, task -> {
                task.getInputFiles().from(SetupUtils.getSetupSourceTarget(clientSource));
                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, clientRuntimeDefinition, clientWorkingDirectory);
            }) : null;

            final TaskProvider<? extends GenerateSourcePatches> createPatches = project.getTasks().register("createSourcePatches", GenerateSourcePatches.class, task -> {
                task.getBase().set(runtimeDefinition.getPatchBase().flatMap(WithOutput::getOutput));
                task.getModified().set(packChanges.flatMap(WithOutput::getOutput));

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
            });

            @Nullable final TaskProvider<? extends GenerateSourcePatches> createClientPatches = splitSourceSets ? project.getTasks().register("createClientSourcePatches", GenerateSourcePatches.class, task -> {
                Validate.notNull(clientPackChanges, "Client pack changes must be present when split source sets are enabled");

                task.getBase().set(clientRuntimeDefinition.getPatchBase().flatMap(WithOutput::getOutput));
                task.getModified().set(clientPackChanges.flatMap(WithOutput::getOutput));

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, clientRuntimeDefinition, clientWorkingDirectory);
            }) : null;

            final TaskProvider<? extends UnpackZip> unpackPatches = project.getTasks().register("unpackSourcePatches", UnpackZip.class, task -> {
                task.getInput().from(project.zipTree(createPatches.flatMap(WithOutput::getOutput)));
                task.getUnpackingTarget().set(mainPatches);
                task.dependsOn(createPatches);

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
            });

            @Nullable final TaskProvider<? extends UnpackZip> unpackClientPatches = splitSourceSets ? project.getTasks().register("unpackClientSourcePatches", UnpackZip.class, task -> {
                Validate.notNull(createClientPatches, "Client create patches must be present when split source sets are enabled");

                task.getInput().from(project.zipTree(createClientPatches.flatMap(WithOutput::getOutput)));
                task.getUnpackingTarget().set(clientPatches);
                task.dependsOn(createClientPatches);

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, clientRuntimeDefinition, clientWorkingDirectory);
            }) : null;

            var mergeMappings = clientRuntimeDefinition.getNeoFormRuntimeDefinition().getTask("mergeMappings");
            Provider<RegularFile> compiledJarProvider;
            Provider<RegularFile> compiledClientJarProvider;
            Provider<RegularFile> compiledJoinedJarProvider;
            if (parchmentArtifact.isPresent()) {
                var officialWithParams = project.getTasks().register(CommonRuntimeUtils.buildTaskName(clientRuntimeDefinition, "officialMappingsJustParameters"), OfficialMappingsJustParameters.class, tsk -> {
                    tsk.getInput().set(mergeMappings.flatMap(WithOutput::getOutput));
                    tsk.dependsOn(mergeMappings);

                    CommonRuntimeExtension.configureCommonRuntimeTaskParameters(tsk, clientRuntimeDefinition, clientWorkingDirectory);
                });
                compiledJarProvider = renameCompiledJar(
                        officialWithParams,
                        project.getTasks().named(mainSource.getJarTaskName(), Jar.class),
                        runtimeDefinition,
                        mainWorkingDirectory
                );
                compiledClientJarProvider = renameCompiledJar(
                        officialWithParams,
                        project.getTasks().named(clientSource.getJarTaskName(), Jar.class),
                        clientRuntimeDefinition,
                        clientWorkingDirectory
                );

                compiledJoinedJarProvider = project.getTasks().register(CommonRuntimeUtils.buildTaskName(runtimeDefinition, "createJoinedJar"), Zip.class, task -> {
                    task.getDestinationDirectory().set(new File(mainWorkingDirectory, "joined"));
                    task.getArchiveFileName().set("joined.jar");

                    task.from(project.zipTree(compiledJarProvider));
                    task.from(project.zipTree(compiledClientJarProvider));
                }).flatMap(Zip::getArchiveFile);
            } else {
                compiledJarProvider = project.getTasks().named(mainSource.getJarTaskName(), Jar.class).flatMap(Jar::getArchiveFile);
                compiledClientJarProvider = splitSourceSets ? project.getTasks().named(clientSource.getJarTaskName(), Jar.class).flatMap(Jar::getArchiveFile) : compiledJarProvider;
                compiledJoinedJarProvider = compiledJarProvider;
            }

            final EnumMap<DistributionType, Provider<RegularFile>> compiledJars = new EnumMap<>(DistributionType.class);
            compiledJars.put(DistributionType.CLIENT, compiledClientJarProvider);
            compiledJars.put(DistributionType.SERVER, compiledJarProvider);
            compiledJars.put(DistributionType.JOINED, compiledJoinedJarProvider);

            final TaskProvider<? extends Jar> sourcesJarProvider = project.getTasks().named(mainSource.getSourcesJarTaskName(), Jar.class);
            sourcesJarProvider.configure(task -> {
                task.exclude("net/minecraft/**");
                task.exclude("com/**");
                task.exclude("mcp/**");
            });

            final TaskProvider<? extends Jar> clientSourcesJarProvider = splitSourceSets ? project.getTasks().named(clientSource.getSourcesJarTaskName(), Jar.class) : sourcesJarProvider;
            clientSourcesJarProvider.configure(task -> {
                task.exclude("net/minecraft/**");
                task.exclude("com/**");
                task.exclude("mcp/**");
            });

            final EnumMap<DistributionType, TaskProvider<GenerateBinaryPatches>> binaryPatchGenerators = new EnumMap<>(DistributionType.class);
            for (DistributionType distribution : DistributionType.values()) {
                final TaskProvider<? extends WithOutput> cleanProvider = cleanProviders.get(distribution);
                final TaskProvider<GenerateBinaryPatches> generateBinaryPatchesTask = project.getTasks().register(distribution.createTaskName("generate", "BinaryPatches"), GenerateBinaryPatches.class, task -> {
                    task.getClean().set(cleanProvider.flatMap(WithOutput::getOutput));
                    task.getPatched().set(compiledJars.get(distribution));
                    task.getDistributionType().set(distribution);

                    if (distribution == DistributionType.JOINED) {
                        task.getPatches().from(mainPatches);
                        task.getPatches().from(clientPatches);
                    } else if (distribution == DistributionType.CLIENT) {
                        task.getPatches().from(clientPatches);
                    } else if (distribution == DistributionType.SERVER) {
                        task.getPatches().from(mainPatches);
                    }

                    task.getMappings().set(obfToMojMappingProviders.get(distribution).flatMap(WithOutput::getOutput));

                    task.mustRunAfter(unpackPatches);
                    if (unpackClientPatches != null) {
                        task.mustRunAfter(unpackClientPatches);
                    }
                    task.mustRunAfter(setupTask);
                    task.mustRunAfter(clientSetupTask);

                    if (distribution != DistributionType.CLIENT) {
                        CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
                    } else {
                        CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, clientRuntimeDefinition, clientWorkingDirectory);
                    }
                });
                binaryPatchGenerators.put(distribution, generateBinaryPatchesTask);
            }

            final Configuration runtimeClasspath = project.getConfigurations().getByName(
                    mainSource.getRuntimeClasspathConfigurationName()
            );
            final Configuration clientRuntimeClasspath = splitSourceSets ? project.getConfigurations().getByName(
                    clientSource.getRuntimeClasspathConfigurationName()
            ) : runtimeClasspath;

            launcherProfile.configure((Action<LauncherProfile>) profile -> {
                profile.getId().set(String.format("%s-%s", project.getName(), project.getVersion()));
                profile.getTime().set(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                profile.getReleaseTime().set(profile.getTime());
                profile.getType().set("release");
                profile.getMainClass().set("cpw.mods.bootstraplauncher.BootstrapLauncher");
                profile.getInheritsFrom().set(runtimeDefinition.getSpecification().getMinecraftVersion());

                //TODO: Deal with logging when model for it stands
                profile.getLoggingConfiguration().set(project.getObjects().newInstance(LauncherProfile.LoggingConfiguration.class));

                final LauncherProfile.Arguments arguments = launcherProfile.getArguments().get();

                arguments.game("--launchTarget");
                arguments.game("forgeclient");

                arguments.jvm("-Djava.net.preferIPv6Addresses=system");
                arguments.jvm(createIgnoreList(project, moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration).map(ignoreList -> "-DignoreList=" + ignoreList + ",${version_name}.jar"));
                arguments.jvm(collectFileNames(mergedModulesConfiguration, project).map(mergedModules -> "-DmergedModules=" + mergedModules));
                arguments.jvm(collectFileNames(clientPluginLayerLibraryConfiguration, project).map(pluginLayerLibraries -> "-Dfml.pluginLayerLibraries=" + pluginLayerLibraries));
                arguments.jvm(collectFileNames(clientGameLayerLibraryConfiguration, project).map(gameLayerLibraries -> "-Dfml.gameLayerLibraries=" + gameLayerLibraries));
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
                arguments.jvm("--add-exports");
                arguments.jvm("jdk.naming.dns/com.sun.jndi.dns=java.naming");

                launcherProfile.getArguments().set(arguments);
            });


            final Configuration launcherJsonInstallerConfiguration = ConfigurationUtils.temporaryUnhandledConfiguration(
                    project.getConfigurations(),
                    "LauncherJsonInstallerConfiguration"
            );
            launcherJsonInstallerConfiguration.extendsFrom(installerConfiguration);
            launcherJsonInstallerConfiguration.shouldResolveConsistentlyWith(clientRuntimeClasspath);

            final ListProperty<URI> repoCollection = new RepositoryCollection(project.getProviders(), project.getObjects(), project.getRepositories()).getURLs();
            final TaskProvider<CreateLauncherJson> createLauncherJson = project.getTasks().register("createLauncherJson", CreateLauncherJson.class, task -> {
                task.getProfile().set(launcherProfile);
                task.getLibraries().from(launcherJsonInstallerConfiguration);
                task.getLibraries().from(clientPluginLayerLibraryConfiguration);
                task.getLibraries().from(clientGameLayerLibraryConfiguration);
                task.getLibraries().from(moduleOnlyConfiguration);
                task.getRepositoryURLs().set(repoCollection);

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, clientRuntimeDefinition, clientWorkingDirectory);
            });

            final TaskProvider<? extends WithOutput> joinedCleanProvider = cleanProviders.get(DistributionType.JOINED);
            final TaskProvider<StripBinPatchedClasses> strippedJar = project.getTasks().register("stripBinaryPatchedClasses", StripBinPatchedClasses.class, task -> {
                task.getCompiled().set(project.getTasks().named(mainSource.getJarTaskName(), Jar.class).flatMap(Jar::getArchiveFile));
                task.getClean().set(joinedCleanProvider.flatMap(WithOutput::getOutput));

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
            });

            final TaskProvider<JarJar> universalJar = project.getTasks().register("universalJar", JarJar.class, task -> {
                task.getArchiveClassifier().set("universal-unsigned");
                task.getArchiveAppendix().set("universal-unsigned");
                task.getArchiveVersion().set(project.getVersion().toString());
                task.getArchiveBaseName().set(project.getName());
                task.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("libs"));
                task.getArchiveFileName().set(project.provider(() -> String.format("%s-%s-universal-unsigned.jar", project.getName(), project.getVersion())));

                task.dependsOn(strippedJar);

                task.from(project.zipTree(strippedJar.flatMap(WithOutput::getOutput)));
                task.manifest(manifest -> {
                    manifest.attributes(ImmutableMap.of("FML-System-Mods", "neoforge"));
                    manifest.attributes(ImmutableMap.of("Specification-Title", "NeoForge", "Specification-Vendor", "NeoForge", "Specification-Version", project.getVersion().toString().substring(0, project.getVersion().toString().lastIndexOf(".")), "Implementation-Title", project.getGroup(), "Implementation-Version", project.getVersion(), "Implementation-Vendor", "NeoForged"), "net/neoforged/neoforge/internal/versions/neoforge/");
                    manifest.attributes(ImmutableMap.of("Specification-Title", "Minecraft", "Specification-Vendor", "Mojang", "Specification-Version", runtimeDefinition.getSpecification().getMinecraftVersion(), "Implementation-Title", "MCP", "Implementation-Version", runtimeDefinition.getSpecification().getVersion(), "Implementation-Vendor", "NeoForged"), "net/neoforged/neoforge/versions/neoform/");
                });

                task.configuration(jarJarConfiguration);
            });

            final TaskProvider<PotentiallySignJar> signUniversalJar = project.getTasks().register("signUniversalJar", PotentiallySignJar.class, task -> {
                task.getInput().set(universalJar.flatMap(Jar::getArchiveFile));
                task.getOutputFileName().set(project.provider(() -> String.format("%s-%s-universal.jar", project.getName(), project.getVersion())));

                task.dependsOn(universalJar);
            });

            installerProfile.configure((Consumer<InstallerProfile>) profile -> {
                profile.getProfile().convention(project.getName());
                profile.getVersion().set(launcherProfile.getId());
                profile.getMinecraft().set(runtimeDefinition.getSpecification().getMinecraftVersion());
                profile.getServerJarPath().set("{LIBRARY_DIR}/net/minecraft/server/{MINECRAFT_VERSION}/server-{MINECRAFT_VERSION}.jar");
                profile.data("MAPPINGS", String.format("[net.neoforged:neoform:%s:mappings@txt]", neoFormVersion), String.format("[net.neoforged:neoform:%s:mappings@txt]", neoFormVersion));
                profile.data("MOJMAPS", String.format("[net.minecraft:client:%s:mappings@txt]", neoFormVersion), String.format("[net.minecraft:server:%s:mappings@txt]", neoFormVersion));
                profile.data("MERGED_MAPPINGS", String.format("[net.neoforged:neoform:%s:mappings-merged@txt]", neoFormVersion), String.format("[net.neoforged:neoform:%s:mappings-merged@txt]", neoFormVersion));
                profile.data("BINPATCH", "/data/client.lzma", "/data/server.lzma");
                profile.data("MC_UNPACKED", String.format("[net.minecraft:client:%s:unpacked]", neoFormVersion), String.format("[net.minecraft:server:%s:unpacked]", neoFormVersion));
                profile.data("MC_SLIM", String.format("[net.minecraft:client:%s:slim]", neoFormVersion), String.format("[net.minecraft:server:%s:slim]", neoFormVersion));
                profile.data("MC_EXTRA", String.format("[net.minecraft:client:%s:extra]", neoFormVersion), String.format("[net.minecraft:server:%s:extra]", neoFormVersion));
                profile.data("MC_SRG", String.format("[net.minecraft:client:%s:srg]", neoFormVersion), String.format("[net.minecraft:server:%s:srg]", neoFormVersion));
                profile.data("PATCHED", String.format("[%s:%s:%s:client]", "net.neoforged", "neoforge", project.getVersion()), String.format("[%s:%s:%s:server]", "net.neoforged", "neoforge", project.getVersion()));
                profile.data("MCP_VERSION", String.format("'%s'", neoFormVersion), String.format("'%s'", neoFormVersion));
                profile.processor(project, Constants.INSTALLERTOOLS, processor -> {
                    processor.server();
                    processor.getArguments().addAll("--task", "EXTRACT_FILES", "--archive", "{INSTALLER}",

                            "--from", "data/run.sh", "--to", "{ROOT}/run.sh", "--exec", "{ROOT}/run.sh",

                            "--from", "data/run.bat", "--to", "{ROOT}/run.bat",

                            "--from", "data/user_jvm_args.txt", "--to", "{ROOT}/user_jvm_args.txt", "--optional", "{ROOT}/user_jvm_args.txt",

                            "--from", "data/win_args.txt", "--to", String.format("{ROOT}/libraries/%s/%s/%s/win_args.txt", project.getGroup().toString().replaceAll("\\.", "/"), project.getName(), project.getVersion()),

                            "--from", "data/unix_args.txt", "--to", String.format("{ROOT}/libraries/%s/%s/%s/unix_args.txt", project.getGroup().toString().replaceAll("\\.", "/"), project.getName(), project.getVersion()));
                });
                profile.processor(project, Constants.INSTALLERTOOLS, processor -> {
                    processor.server();
                    processor.getArguments().addAll("--task", "BUNDLER_EXTRACT", "--input", "{MINECRAFT_JAR}", "--output", "{ROOT}/libraries/", "--libraries");
                });
                profile.processor(project, Constants.INSTALLERTOOLS, processor -> {
                    processor.server();
                    processor.getArguments().addAll("--task", "BUNDLER_EXTRACT", "--input", "{MINECRAFT_JAR}", "--output", "{MC_UNPACKED}", "--jar-only");
                });
                profile.processor(project, Constants.INSTALLERTOOLS, processor -> {
                    processor.getArguments().addAll("--task", "MCP_DATA", "--input", String.format("[%s]", neoFormDependency), "--output", "{MAPPINGS}", "--key", "mappings");
                });
                profile.processor(project, Constants.INSTALLERTOOLS, processor -> {
                    processor.getArguments().addAll("--task", "DOWNLOAD_MOJMAPS", "--version", runtimeDefinition.getSpecification().getMinecraftVersion(), "--side", "{SIDE}", "--output", "{MOJMAPS}");
                });
                profile.processor(project, Constants.INSTALLERTOOLS, processor -> {
                    processor.getArguments().addAll("--task", "MERGE_MAPPING", "--left", "{MAPPINGS}", "--right", "{MOJMAPS}", "--output", "{MERGED_MAPPINGS}", "--classes", "--fields", "--methods", "--reverse-right");
                });
                profile.processor(project, Constants.JARSPLITTER, processor -> {
                    processor.client();
                    processor.getArguments().addAll("--input", "{MINECRAFT_JAR}", "--slim", "{MC_SLIM}", "--extra", "{MC_EXTRA}", "--srg", "{MERGED_MAPPINGS}");
                });
                profile.processor(project, Constants.JARSPLITTER, processor -> {
                    processor.server();
                    processor.getArguments().addAll("--input", "{MC_UNPACKED}", "--slim", "{MC_SLIM}", "--extra", "{MC_EXTRA}", "--srg", "{MERGED_MAPPINGS}");
                });
                profile.processor(project, Constants.FART, processor -> {
                    processor.getArguments().addAll("--input", "{MC_SLIM}", "--output", "{MC_SRG}", "--names", "{MERGED_MAPPINGS}", "--ann-fix", "--ids-fix", "--src-fix", "--record-fix");
                });
                profile.processor(project, Constants.BINARYPATCHER, processor -> {
                    processor.getArguments().addAll("--clean", "{MC_SRG}", "--output", "{PATCHED}", "--apply", "{BINPATCH}");
                });

                profile.getLibraries().add(Library.fromOutput(signUniversalJar, project, "net.neoforged", "neoforge", project.getVersion().toString(), "universal"));

                //TODO: Abstract this away to some kind of DSL property
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
                profile.getWelcome().convention(profile.getProfile().map(name -> "Welcome to the simple " + name + " installer"));

                profile.getShouldHideExtract().set(true);
            });

            final Configuration installerJsonInstallerLibrariesConfiguration = ConfigurationUtils.temporaryUnhandledConfiguration(
                    project.getConfigurations(),
                    "InstallerJsonInstallerLibraries"
            );
            installerJsonInstallerLibrariesConfiguration.extendsFrom(installerLibrariesConfiguration);
            installerJsonInstallerLibrariesConfiguration.shouldResolveConsistentlyWith(runtimeClasspath);

            final TaskProvider<CreateLegacyInstallerJson> createLegacyInstallerJson = project.getTasks().register("createLegacyInstallerJson", CreateLegacyInstallerJson.class, task -> {
                task.getProfile().set(installerProfile);
                task.getLibraries().from(installerJsonInstallerLibrariesConfiguration);
                task.getRepositoryURLs().set(repoCollection);

                task.dependsOn(signUniversalJar);

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
            });

            final Configuration installerToolConfiguration = ConfigurationUtils.temporaryConfiguration(
                    project,
                    "PlatformConfigInstallerLookup",
                    project.getDependencies().create("net.neoforged:legacyinstaller:3.0.+:shrunk"));
            final TaskProvider<Download> downloadInstaller = project.getTasks().register("downloadInstaller", Download.class, task -> {
                task.getInput().from(installerToolConfiguration);
                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
            });

            final TaskProvider<CreateClasspathFiles> createWindowsServerArgsFile = project.getTasks().register("createWindowsServerArgsFile", CreateClasspathFiles.class, task -> {
                task.getModulePath().from(moduleOnlyConfiguration);
                task.getClasspath().from(installerConfiguration);
                task.getClasspath().from(gameLayerLibraryConfiguration);
                task.getClasspath().from(pluginLayerLibraryConfiguration);
                task.getPathSeparator().set(";");
                task.getServer().set(runtimeDefinition.getGameArtifactProvidingTasks().get(GameArtifact.SERVER_JAR).flatMap(WithOutput::getOutput));
                task.getNeoFormVersion().set(neoFormVersion);

                configureInstallerTokens(task, runtimeDefinition, Lists.newArrayList(moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration), pluginLayerLibraryConfiguration, gameLayerLibraryConfiguration);

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
            });

            final TaskProvider<CreateClasspathFiles> createUnixServerArgsFile = project.getTasks().register("createUnixServerArgsFile", CreateClasspathFiles.class, task -> {
                task.getModulePath().from(moduleOnlyConfiguration);
                task.getClasspath().from(installerConfiguration);
                task.getClasspath().from(gameLayerLibraryConfiguration);
                task.getClasspath().from(pluginLayerLibraryConfiguration);
                task.getPathSeparator().set(":");
                task.getServer().set(runtimeDefinition.getGameArtifactProvidingTasks().get(GameArtifact.SERVER_JAR).flatMap(WithOutput::getOutput));
                task.getNeoFormVersion().set(neoFormVersion);

                configureInstallerTokens(task, runtimeDefinition, Lists.newArrayList(moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration), pluginLayerLibraryConfiguration, gameLayerLibraryConfiguration);

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
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

                configureInstallerTokens(task, runtimeDefinition, Lists.newArrayList(moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration), pluginLayerLibraryConfiguration, gameLayerLibraryConfiguration);

                if (project.getProperties().containsKey("neogradle.runtime.platform.installer.debug") && Boolean.parseBoolean(project.getProperties().get("neogradle.runtime.platform.installer.debug").toString())) {
                    task.from(signUniversalJar.flatMap(WithOutput::getOutput), spec -> {
                        spec.into(String.format("/maven/net/neoforged/neoforge/%s/", project.getVersion()));
                        spec.rename(name -> String.format("neoforge-%s-universal.jar", project.getVersion()));
                    });
                }
            });

            TaskProvider<PotentiallySignJar> signInstallerJar = project.getTasks().register("signInstallerJar", PotentiallySignJar.class, task -> {
                task.getInput().set(installerJar.flatMap(Zip::getArchiveFile));
                task.getOutputFileName().set(project.provider(() -> String.format("%s-%s-installer.jar", project.getName(), project.getVersion())));

                task.dependsOn(installerJar);
            });

            //Note the following runtypes are for now hardcoded, in the future they should be pulled from the runtime definition
            //Note: We can not use a 'configureEach' here, because this causes issues with the config cache.
            userdevProfile.runType("client", type -> {
                type.getEnvironmentVariables().put("MOD_CLASSES", "{source_roots}");

                type.getIsClient().set(true);
                type.getIsGameTest().set(true);
                type.getSystemProperties().put("neoforge.enableGameTest", "true");

                type.getArguments().add("--launchTarget");
                type.getArguments().add("forgeclientuserdev");
                type.getArguments().add("--version");
                type.getArguments().add(project.getVersion().toString());
                type.getArguments().add("--assetIndex");
                type.getArguments().add("{asset_index}");
                type.getArguments().add("--assetsDir");
                type.getArguments().add("{assets_root}");

                configureUserdevRunType(type, moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, userdevCompileOnlyConfiguration, project);
            });
            userdevProfile.runType("server", type -> {
                type.getEnvironmentVariables().put("MOD_CLASSES", "{source_roots}");

                type.getIsServer().set(true);

                type.getArguments().add("--launchTarget");
                type.getArguments().add("forgeserveruserdev");

                configureUserdevRunType(type, moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, userdevCompileOnlyConfiguration, project);
            });
            userdevProfile.runType("gameTestServer", type -> {
                type.getEnvironmentVariables().put("MOD_CLASSES", "{source_roots}");

                type.getIsServer().set(true);
                type.getIsGameTest().set(true);
                type.getSystemProperties().put("neoforge.enableGameTest", "true");
                type.getSystemProperties().put("neoforge.gameTestServer", "true");


                type.getArguments().add("--launchTarget");
                type.getArguments().add("forgeserveruserdev");

                configureUserdevRunType(type, moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, userdevCompileOnlyConfiguration, project);
            });
            userdevProfile.runType("data", type -> {
                type.getEnvironmentVariables().put("MOD_CLASSES", "{source_roots}");

                type.getIsDataGenerator().set(true);

                type.getArguments().add("--launchTarget");
                type.getArguments().add("forgedatauserdev");
                type.getArguments().add("--assetIndex");
                type.getArguments().add("{asset_index}");
                type.getArguments().add("--assetsDir");
                type.getArguments().add("{assets_root}");

                configureUserdevRunType(type, moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, userdevCompileOnlyConfiguration, project);
            });

            userdevProfile.runType("junit", type -> {
                type.getEnvironmentVariables().put("MOD_CLASSES", "{source_roots}");
                type.getEnvironmentVariables().put("MCP_MAPPINGS", "{mcp_mappings}");

                type.getIsClient().set(true);
                type.getIsJUnit().set(true);

                type.getArguments().add("--launchTarget");
                type.getArguments().add("forgejunituserdev");
                type.getArguments().add("--version");
                type.getArguments().add(project.getVersion().toString());
                type.getArguments().add("--assetIndex");
                type.getArguments().add("{asset_index}");
                type.getArguments().add("--assetsDir");
                type.getArguments().add("{assets_root}");

                configureUserdevRunType(type, moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, userdevCompileOnlyConfiguration, project);
            });

            userdevProfile.getNeoForm().set(neoFormDependency);
            userdevProfile.getSourcePatchesDirectory().set("patches/");
            userdevProfile.getAccessTransformerDirectory().set("ats/");
            userdevProfile.getBinaryPatchFile().set("joined.lzma");
            userdevProfile.getBinaryPatcher().set(project.getObjects().newInstance(UserdevProfile.ToolExecution.class).configure((Action<UserdevProfile.ToolExecution>) tool -> {
                tool.getTool().set(Constants.BINPATCHER);
                tool.getArguments().addAll("--clean", "{clean}", "--output", "{output}", "--apply", "{patch}");
            }));
            userdevProfile.getSourcesJarArtifactCoordinate().set(createCoordinate(project, "sources"));
            userdevProfile.getUniversalJarArtifactCoordinate().set(createCoordinate(project, "universal"));

            final Configuration userdevJsonLibrariesConfiguration = ConfigurationUtils.temporaryUnhandledConfiguration(
                    project.getConfigurations(),
                    "userdevLibraries"
            );
            userdevJsonLibrariesConfiguration.extendsFrom(
                    userdevCompileOnlyConfiguration,
                    installerLibrariesConfiguration,
                    gameLayerLibraryConfiguration,
                    pluginLayerLibraryConfiguration,
                    moduleOnlyConfiguration
            );
            userdevJsonLibrariesConfiguration.shouldResolveConsistentlyWith(runtimeClasspath);

            final Configuration userdevJsonModuleOnlyConfiguration = ConfigurationUtils.temporaryUnhandledConfiguration(
                    project.getConfigurations(),
                    "userdevModuleOnly"
            );
            userdevJsonModuleOnlyConfiguration.extendsFrom(moduleOnlyConfiguration);
            userdevJsonLibrariesConfiguration.shouldResolveConsistentlyWith(runtimeClasspath);

            final Configuration userdevJsonUserdevTestImplementationConfiguration = ConfigurationUtils.temporaryUnhandledConfiguration(
                    project.getConfigurations(),
                    "userdevJsonUserdevTestImplementation"
            );
            userdevJsonUserdevTestImplementationConfiguration.extendsFrom(userdevTestImplementationConfiguration);
            userdevJsonUserdevTestImplementationConfiguration.shouldResolveConsistentlyWith(runtimeClasspath);

            final TaskProvider<CreateUserdevJson> createUserdevJson = project.getTasks().register("createUserdevJson", CreateUserdevJson.class, task -> {
                task.getProfile().set(userdevProfile);
                task.getLibraries().from(userdevJsonLibrariesConfiguration);
                task.getModules().from(userdevJsonModuleOnlyConfiguration);

                task.getTestLibraries().from(userdevJsonUserdevTestImplementationConfiguration);

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
            });

            final TaskProvider<AccessTransformerFileGenerator> generateAts = project.getTasks().register("generateAccessTransformers", AccessTransformerFileGenerator.class, task -> {
                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
            });

            final TaskProvider<PackJar> packPatches = project.getTasks().register("packPatches", PackJar.class, task -> {
                task.getInputFiles().from(project.fileTree(mainPatches).matching(filterable -> {
                    filterable.include("**/*.patch");
                }));

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
            });

            @Nullable final TaskProvider<PackJar> clientPackPatches = splitSourceSets ? project.getTasks().register("clientPackPatches", PackJar.class, task -> {
                task.getInputFiles().from(project.fileTree(clientPackChanges).matching(filterable -> {
                    filterable.include("**/*.patch");
                }));

                CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, clientRuntimeDefinition, clientWorkingDirectory);
            }) : null;

            final TaskProvider<? extends WithOutput> bakePatches;
            @Nullable final TaskProvider<? extends WithOutput> bakeClientPatches;
            if (!parchmentArtifact.isPresent()) {
                bakePatches = project.getTasks().register("bakePatches", BakePatches.class, task -> {
                    task.getInput().set(packPatches.flatMap(WithOutput::getOutput));

                    CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
                });
                bakeClientPatches = splitSourceSets ? project.getTasks().register("bakeClientPatches", BakePatches.class, task -> {
                    task.getInput().set(clientPackPatches.flatMap(WithOutput::getOutput));

                    CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, clientRuntimeDefinition, clientWorkingDirectory);
                }) : null;
            } else {
                final var sourceSetDir = SetupUtils.getSetupSourceTarget(mainSource);
                final var sourcesWithoutParchment = RuntimeDevRuntimeExtension.applyParchment(
                        project,
                        "reverseParchment",
                        mergeMappings.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile),
                        project.provider(() -> ""),
                        project.provider(() -> sourceSetDir),
                        false,
                        runtimeDefinition.getSpecification(),
                        mainWorkingDirectory,
                        strippedJar
                );
                sourcesWithoutParchment.configure(withOutput -> withOutput.getProgramArguments().add("--ignore-prefix=mcp/"));
                bakePatches = project.getTasks().register("createRenamedSourcePatches", GenerateSourcePatches.class, task -> {
                    task.getBase().set(neoFormSources.flatMap(WithOutput::getOutput));
                    task.getModified().set(sourcesWithoutParchment.flatMap(WithOutput::getOutput));
                    task.getShouldCreateAutomaticHeader().set(false);

                    CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, mainWorkingDirectory);
                });

                if (splitSourceSets) {
                    final var sourceSetClientDir = SetupUtils.getSetupSourceTarget(clientSource);
                    final var sourcesWithoutParchmentClient = RuntimeDevRuntimeExtension.applyParchment(
                            project,
                            "reverseParchmentClient",
                            mergeMappings.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile),
                            project.provider(() -> ""),
                            project.provider(() -> sourceSetClientDir),
                            false,
                            clientRuntimeDefinition.getSpecification(),
                            clientWorkingDirectory,
                            strippedJar
                    );
                    sourcesWithoutParchmentClient.configure(withOutput -> withOutput.getProgramArguments().add("--ignore-prefix=mcp/"));
                    bakeClientPatches = project.getTasks().register("createRenamedSourcePatchesClient", GenerateSourcePatches.class, task -> {
                        task.getBase().set(clientNeoFormSources.flatMap(WithOutput::getOutput));
                        task.getModified().set(sourcesWithoutParchmentClient.flatMap(WithOutput::getOutput));
                        task.getShouldCreateAutomaticHeader().set(false);

                        CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, clientRuntimeDefinition, clientWorkingDirectory);
                    });
                } else {
                    bakeClientPatches = null;
                }
            }

            final AccessTransformers accessTransformers = project.getExtensions().getByType(AccessTransformers.class);

            final TaskProvider<Jar> userdevJar = project.getTasks().register("userdevJar", Jar.class, task -> {
                task.getArchiveClassifier().set("userdev");
                task.getArchiveAppendix().set("userdev");
                task.getArchiveVersion().set(project.getVersion().toString());
                task.getArchiveBaseName().set(project.getName());
                task.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("libs"));
                task.getArchiveFileName().set(project.provider(() -> String.format("%s-%s-userdev.jar", project.getName(), project.getVersion())));

                task.dependsOn(bakePatches);
                if (bakeClientPatches != null) {
                    task.dependsOn(bakeClientPatches);
                }

                //We need to get a raw file tree here, because else we capture the task reference in copy spec.
                final FileTree bakedPatches = project.zipTree(bakePatches.get().getOutput().get().getAsFile());
                task.from(createUserdevJson.flatMap(WithOutput::getOutput), spec -> {
                    spec.rename(name -> "config.json");
                });
                task.from(generateAts.flatMap(WithOutput::getOutput), spec -> {
                    spec.into("ats/");
                });
                task.from(accessTransformers.getFiles(), spec -> {
                    spec.into("ats/");
                });
                task.from(binaryPatchGenerators.get(DistributionType.JOINED).flatMap(WithOutput::getOutput), spec -> {
                    spec.rename(name -> "joined.lzma");
                });
                task.from(bakedPatches, spec -> {
                    spec.into("patches/");
                });
                if (bakeClientPatches != null) {
                    task.from(project.zipTree(bakeClientPatches.get().getOutput().get().getAsFile()), spec -> {
                        spec.into("client_patches/");
                    });
                }
            });

            final TaskProvider<?> assembleTask = project.getTasks().named("assemble");
            assembleTask.configure(task -> {
                task.dependsOn(signInstallerJar);
                task.dependsOn(signUniversalJar);
                task.dependsOn(userdevJar);
                task.dependsOn(sourcesJarProvider);
            });
        });
    }

    private TaskProvider<SetupProjectFromRuntime> configureSetupTasks(
            Provider<RegularFile> rawJarProvider,
            SourceSet mainSource,
            Configuration minecraftDependencies) {
        final IdeManagementExtension ideManagementExtension = project.getExtensions().getByType(IdeManagementExtension.class);

        final TaskProvider<? extends Task> ideImportTask = ideManagementExtension.getOrCreateIdeImportTask();

        final TaskProvider<SetupProjectFromRuntime> projectSetup = project.getTasks().register(mainSource.getTaskName("setup", "Project"), SetupProjectFromRuntime.class, task -> {
            task.getSourcesFile().set(rawJarProvider);
            task.getSourcesDirectory().set(SetupUtils.getSetupSourceTarget(mainSource));
            task.getResourcesDirectory().set(SetupUtils.getSetupResourcesTarget(mainSource));
            task.dependsOn(ideImportTask);
        });

        final Configuration apiConfiguration = project.getConfigurations().getByName(mainSource.getApiConfigurationName());
        minecraftDependencies.getAllDependencies().forEach(dep -> apiConfiguration.getDependencies().add(dep));

        final Project rootProject = project.getRootProject();
        if (!rootProject.getTasks().getNames().contains("setup")) {
            rootProject.getTasks().create("setup");
        }

        rootProject.getTasks().named("setup").configure(task -> task.dependsOn(projectSetup));

        return projectSetup;
    }

    private void configureRunType(final Project project,
                                  final RunType runType,
                                  final boolean splitSourceSets,
                                  final Configuration mergedModuleConfiguration,
                                  final Configuration moduleOnlyConfiguration,
                                  final Configuration gameLayerLibraryConfiguration,
                                  final Configuration pluginLayerLibraryConfiguration) {
        runType.getMainClass().set("cpw.mods.bootstraplauncher.BootstrapLauncher");

        runType.getSystemProperties().put("java.net.preferIPv6Addresses", "system");
        runType.getJvmArguments().addAll("-p", moduleOnlyConfiguration.getAsPath());

        runType.getSystemProperties().put("ignoreList", createIgnoreList(project, moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration));
        runType.getSystemProperties().put("mergeModules", collectFileNames(mergedModuleConfiguration, project));
        runType.getSystemProperties().put("fml.pluginLayerLibraries", collectFileNames(pluginLayerLibraryConfiguration, project));
        runType.getSystemProperties().put("fml.gameLayerLibraries", collectFileNames(gameLayerLibraryConfiguration, project));
        runType.getSystemProperties().put("legacyClassPath", project.getConfigurations().getByName("runtimeClasspath").getAsPath());
        runType.getJvmArguments().addAll("--add-modules", "ALL-MODULE-PATH");
        runType.getJvmArguments().addAll("--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler");
        runType.getJvmArguments().addAll("--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler");
        runType.getJvmArguments().addAll("--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler");
        runType.getJvmArguments().addAll("--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming");

        runType.getEnvironmentVariables().put("NEOFORGE_SPEC", project.getVersion().toString().substring(0, project.getVersion().toString().lastIndexOf(".")));

        runType.getClasspath().from(runType.getIsClient().map(isClient -> {
            final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
            final SourceSet sourceSet = javaPluginExtension.getSourceSets().getByName(splitSourceSets ? "client" : "main");
            return sourceSet.getRuntimeClasspath();
        }));
    }

    private static void configureInstallerTokens(final TokenizedTask tokenizedTask, final RuntimeDevRuntimeDefinition runtimeDefinition, final Collection<Configuration> ignoreConfigurations, final Configuration pluginLayerLibraries, final Configuration gameLayerLibraries) {
        //TODO: This should be moved to a DSL configurable model.

        tokenizedTask.token("TASK", "forgeserver");
        tokenizedTask.token("MAVEN_PATH", String.format("%s/%s/%s", tokenizedTask.getProject().getGroup().toString().replace('.', '/'), tokenizedTask.getProject().getName(), tokenizedTask.getProject().getVersion()));
        tokenizedTask.token("FORGE_VERSION", tokenizedTask.getProject().getVersion());
        tokenizedTask.token("FML_VERSION", tokenizedTask.getProject().getProperties().get("fancy_mod_loader_version"));
        tokenizedTask.token("MC_VERSION", runtimeDefinition.getSpecification().getMinecraftVersion());
        tokenizedTask.token("MCP_VERSION", extractNeoformVersion(runtimeDefinition));
        tokenizedTask.token("FORGE_GROUP", tokenizedTask.getProject().getGroup());
        tokenizedTask.token("IGNORE_LIST", ignoreConfigurations.stream().flatMap(config -> config.getFiles().stream()).map(File::getName).collect(Collectors.joining(",")));
        tokenizedTask.token("PLUGIN_LAYER_LIBRARIES", pluginLayerLibraries.getFiles().stream().map(File::getName).collect(Collectors.joining(",")));
        tokenizedTask.token("GAME_LAYER_LIBRARIES", gameLayerLibraries.getFiles().stream().map(File::getName).collect(Collectors.joining(",")));
        tokenizedTask.token("MODULES", "ALL-MODULE-PATH");
    }

    private static String extractNeoformVersion(RuntimeDevRuntimeDefinition runtimeDefinition) {
        final String completeVersion = runtimeDefinition.getNeoFormRuntimeDefinition().getSpecification().getNeoFormVersion();
        return completeVersion.substring(completeVersion.lastIndexOf("-") + 1);
    }

    private static Provider<String> collectFileNames(Configuration config, Project project) {
        return project.provider(() -> config.getFiles().stream().map(File::getName).collect(Collectors.joining(",")));
    }

    @NotNull
    private static Provider<String> createIgnoreList(Project project, Configuration... configurations) {
        return project.provider(() -> {
            StringBuilder ignoreList = new StringBuilder(1000);
            for (Configuration cfg : configurations) {
                if (!cfg.isEmpty()) { // Skip empty else we will end up with ",," in the ignore list and all entries will be ignored.
                    ignoreList.append(cfg.getFiles().stream().map(File::getName).collect(Collectors.joining(","))).append(",");
                }
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

    private void configureRun(final Run run, final boolean splitSourceSet, final RuntimeDevRuntimeDefinition runtimeDefinition) {
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final SourceSet mainSourceSet = javaPluginExtension.getSourceSets().getByName("main");

        run.getConfigureAutomatically().set(true);
        run.getConfigureFromDependencies().set(false);

        run.getDependsOn().addAll(
                TransformerUtils.ifTrue(run.getIsClient(), runtimeDefinition.getAssets(), runtimeDefinition.getNatives())
        );

        run.getArguments().addAll(
                TransformerUtils.ifTrue(run.getIsClient(),
                        "--username", "Dev",
                        "--version", project.getName(),
                        "--accessToken", "0",
                        "--launchTarget", "forgeclientdev")

        );

        run.getArguments().addAll(
                TransformerUtils.ifTrue(run.getIsServer(),
                        "--launchTarget", "forgeserverdev")
        );

        run.getSystemProperties().putAll(
                TransformerUtils.ifTrueMap(run.getIsGameTest(),
                        "neoforge.enableGameTest", "true")
        );

        run.getSystemProperties().putAll(
                TransformerUtils.ifTrueMap(
                        run.getIsGameTest().flatMap(TransformerUtils.and(run.getIsServer())),
                        "neoforge.gameTestServer", "true")
        );

        run.getArguments().addAll(
                TransformerUtils.ifTrue(run.getIsDataGenerator(),
                        "--launchTarget", "forgedatadev",
                        "--flat", "--all", "--validate",
                        "--output", project.getRootProject().file("src/generated/resources/").getAbsolutePath())
        );

        mainSourceSet.getResources().getSrcDirs().forEach(file -> {
            run.getArguments().addAll(
                    TransformerUtils.ifTrue(run.getIsDataGenerator(),
                            "--existing", file.getAbsolutePath())
            );
        });

        if (splitSourceSet) {
            final SourceSet clientSourceSet = javaPluginExtension.getSourceSets().getByName("main");
            clientSourceSet.getResources().getSrcDirs().forEach(file -> {
                run.getArguments().addAll(
                        TransformerUtils.ifTrue(run.getIsDataGenerator(),
                                "--existing", file.getAbsolutePath())
                );
            });
        }

        Provider<String> assetsDir = DownloadAssets.getAssetsDirectory(project).map(Directory::getAsFile).map(File::getAbsolutePath);
        Provider<String> assetIndex = runtimeDefinition.getAssets().flatMap(DownloadAssets::getAssetIndex);

        run.getArguments().addAll(
                TransformerUtils.ifTrue(
                        run.getIsDataGenerator().flatMap(TransformerUtils.or(run.getIsClient(), run.getIsJUnit())),
                        project.provider(() -> "--assetsDir"),
                        assetsDir,
                        project.provider(() -> "--assetIndex"),
                        assetIndex)
        );

        run.getArguments().addAll(
                TransformerUtils.ifTrue(run.getIsJUnit(),
                        "--launchTarget", "forgejunitdev")
        );
    }

    private TaskProvider<? extends WithOutput> createCleanProvider(final TaskProvider<? extends WithOutput> jarProvider, final RuntimeDevRuntimeDefinition runtimeDefinition, File workingDirectory) {
        final RuntimeDevRuntimeSpecification spec = runtimeDefinition.getSpecification();

        final Provider<Map<String, String>> versionData = project.provider(() -> {
            final Map<String, String> data = Maps.newHashMap();
            final Mappings mappingsExtension = project.getExtensions().getByType(Mappings.class);
            final Map<String, String> mappingVersionData = Maps.newHashMap();
            mappingVersionData.put(NamingConstants.Version.VERSION, runtimeDefinition.getSpecification().getMinecraftVersion());
            mappingVersionData.put(NamingConstants.Version.MINECRAFT_VERSION, runtimeDefinition.getSpecification().getMinecraftVersion());
            mappingVersionData.putAll(mappingsExtension.getVersion().get());
            return data;
        });

        final Set<TaskProvider<? extends Runtime>> additionalRuntimeTasks = Sets.newHashSet();
        final TaskBuildingContext context = new TaskBuildingContext(spec.getProject(), String.format("mapCleanFor%s", StringUtils.capitalize(jarProvider.getName())), taskName -> CommonRuntimeUtils.buildTaskName(spec, taskName), jarProvider, runtimeDefinition.getGameArtifactProvidingTasks(), versionData, additionalRuntimeTasks, runtimeDefinition);

        final TaskProvider<? extends Runtime> remapTask = context.getNamingChannel().getApplyCompiledMappingsTaskBuilder().get().build(context);
        additionalRuntimeTasks.forEach(taskProvider -> taskProvider.configure(task -> CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory)));
        remapTask.configure(task -> CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory));

        return remapTask;
    }

    private TaskProvider<? extends WithOutput> createFlippedMojMapProvider(final TaskProvider<? extends WithOutput> mojmapProvider, final RuntimeDevRuntimeDefinition runtimeDefinition, File workingDirectory) {
        final String taskName = String.format("invert%s", StringUtils.capitalize(mojmapProvider.getName()));
        if (project.getTasks().getNames().contains(taskName)) {
            return project.getTasks().named(taskName, WriteIMappingsFile.class);
        }

        return project.getTasks().register(taskName, WriteIMappingsFile.class, task -> {
            task.getMappings().set(mojmapProvider.flatMap(WithOutput::getOutput).map(TransformerUtils.guard(file -> IMappingFile.load(file.getAsFile()))).map(file -> new CacheableIMappingFile(file.reverse())));

            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        });
    }

    private Provider<RegularFile> renameCompiledJar(TaskProvider<? extends WithOutput> mappingsFile,
                                                    TaskProvider<? extends Jar> input,
                                                    final RuntimeDevRuntimeDefinition runtimeDefinition,
                                                    final File workingDirectory) {
        var inputFile = input.flatMap(Jar::getArchiveFile);
        return project.getTasks().register(CommonRuntimeUtils.buildTaskName(runtimeDefinition, "renameCompiledJar"), DefaultExecute.class, task -> {
            task.getArguments().putRegularFile("mappings", mappingsFile.flatMap(WithOutput::getOutput));
            task.getArguments().putRegularFile("input", inputFile);

            task.getExecutingJar().set(ToolUtilities.resolveTool(project, Constants.FART));
            task.getProgramArguments().addAll("--names", "{mappings}");
            task.getProgramArguments().addAll("--input", "{input}");
            task.getProgramArguments().addAll("--output", "{output}");
            task.getProgramArguments().add("--disable-abstract-param");

            project.getExtensions().getByType(SourceSetContainer.class).getByName("main").getCompileClasspath()
                    .forEach(f -> task.getProgramArguments().addAll("--lib", f.getAbsolutePath()));

            task.dependsOn(input);
            task.dependsOn(mappingsFile);

            CommonRuntimeExtension.configureCommonRuntimeTaskParameters(task, runtimeDefinition, workingDirectory);
        }).flatMap(WithOutput::getOutput);
    }

    private void configureUserdevRunType(final RunType runType, Configuration moduleOnlyConfiguration, Configuration gameLayerLibraryConfiguration, Configuration pluginLayerLibraryConfiguration, Configuration userdevCompileOnlyConfiguration, Project project) {
        runType.getMainClass().set("cpw.mods.bootstraplauncher.BootstrapLauncher");

        runType.getArguments().addAll("--gameDir", ".");

        runType.getSystemProperties().put("java.net.preferIPv6Addresses", "system");
        runType.getSystemProperties().put("ignoreList", createIgnoreList(project, moduleOnlyConfiguration, gameLayerLibraryConfiguration, pluginLayerLibraryConfiguration, userdevCompileOnlyConfiguration));

        runType.getSystemProperties().put("fml.pluginLayerLibraries", collectFileNames(pluginLayerLibraryConfiguration, project));
        runType.getSystemProperties().put("fml.gameLayerLibraries", collectFileNames(gameLayerLibraryConfiguration, project));
        runType.getSystemProperties().put("mergeModules", "jna-5.10.0.jar,jna-platform-5.10.0.jar");
        runType.getSystemProperties().put("legacyClassPath.file", "{minecraft_classpath_file}");
        runType.getJvmArguments().addAll("-p", "{modules}");
        runType.getJvmArguments().addAll("--add-modules", "ALL-MODULE-PATH");
        runType.getJvmArguments().addAll("--add-opens", "java.base/java.util.jar=cpw.mods.securejarhandler");
        runType.getJvmArguments().addAll("--add-opens", "java.base/java.lang.invoke=cpw.mods.securejarhandler");
        runType.getJvmArguments().addAll("--add-exports", "java.base/sun.security.util=cpw.mods.securejarhandler");
        runType.getJvmArguments().addAll("--add-exports", "jdk.naming.dns/com.sun.jndi.dns=java.naming");
    }

    private static String createCoordinate(final Project project, final String classifier) {
        return Objects.toString(Artifact.from(project, classifier, "jar"));
    }

    @NotNull
    public DynamicProjectType getType() {
        if (type == null) throw new IllegalStateException("Project is not configured yet!");
        return type;
    }

    @DSLProperty
    public abstract Property<Boolean> getIsUpdating();

    @Inject
    public abstract ProviderFactory getProviderFactory();

    @DSLProperty
    public abstract Property<String> getParchment();
}
