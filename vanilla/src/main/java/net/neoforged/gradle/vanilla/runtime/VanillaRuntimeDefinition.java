package net.neoforged.gradle.vanilla.runtime;

import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.vanilla.runtime.spec.VanillaRuntimeSpecification;
import net.neoforged.gradle.vanilla.util.InterpolationConstants;
import net.neoforged.gradle.vanilla.util.ServerLaunchInformation;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Represents a configured and registered runtime for vanilla.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class VanillaRuntimeDefinition extends CommonRuntimeDefinition<VanillaRuntimeSpecification> {

    private final TaskProvider<DownloadAssets> assetsTaskProvider;
    private final TaskProvider<ExtractNatives> nativesTaskProvider;
    private final Optional<ServerLaunchInformation> serverLaunchInformation;

    public VanillaRuntimeDefinition(@NotNull VanillaRuntimeSpecification specification,
                                    @NotNull LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs,
                                    @NotNull TaskProvider<? extends ArtifactProvider> sourceJarTask,
                                    @NotNull TaskProvider<? extends ArtifactProvider> rawJarTask,
                                    @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactProvidingTasks,
                                    @NotNull Configuration minecraftDependenciesConfiguration,
                                    @NotNull Consumer<TaskProvider<? extends Runtime>> associatedTaskConsumer,
                                    Provider<VersionJson> versionJson,
                                    TaskProvider<DownloadAssets> assetsTaskProvider,
                                    TaskProvider<ExtractNatives> nativesTaskProvider,
                                    Optional<ServerLaunchInformation> serverLaunchInformation) {
        super(specification, taskOutputs, sourceJarTask, rawJarTask, gameArtifactProvidingTasks, minecraftDependenciesConfiguration, associatedTaskConsumer, versionJson);
        this.assetsTaskProvider = assetsTaskProvider;
        this.nativesTaskProvider = nativesTaskProvider;
        this.serverLaunchInformation = serverLaunchInformation;
    }

    @Override
    @NotNull
    public TaskProvider<DownloadAssets> getAssets() {
        return assetsTaskProvider;
    }

    @Override
    @NotNull
    public TaskProvider<ExtractNatives> getNatives() {
        return nativesTaskProvider;
    }

    @Override
    @NotNull
    public TaskProvider<? extends WithOutput> getListLibrariesTaskProvider() {
        return this.getTask("libraries");
    }

    public Optional<ServerLaunchInformation> getServerLaunchInformation() {
        return serverLaunchInformation;
    }

    @Override
    protected void buildRunInterpolationData(RunImpl run, MapProperty<String, String> interpolationData) {
        final String runtimeVersion = this.getClass().getPackage().getImplementationVersion();

        interpolationData.put(InterpolationConstants.VERSION_NAME, getSpecification().getMinecraftVersion());
        interpolationData.put(InterpolationConstants.ASSETS_ROOT, DownloadAssets.getAssetsDirectory(run.getProject()).map(Directory::getAsFile).map(File::getAbsolutePath));
        interpolationData.put(InterpolationConstants.ASSETS_INDEX_NAME, getAssets().flatMap(DownloadAssets::getAssetIndexFile).map(RegularFile::getAsFile).map(File::getName).map(s -> s.substring(0, s.lastIndexOf('.'))));
        interpolationData.put(InterpolationConstants.AUTH_ACCESS_TOKEN, "0");
        interpolationData.put(InterpolationConstants.USER_TYPE, "legacy");
        interpolationData.put(InterpolationConstants.VERSION_TYPE, getVersionJson().map(VersionJson::getType));
        interpolationData.put(InterpolationConstants.NATIVES_DIRECTORY, getNatives().flatMap(ExtractNatives::getOutputDirectory).map(Directory::getAsFile).map(File::getAbsolutePath));
        interpolationData.put(InterpolationConstants.LAUNCHER_NAME, "NeoGradle-Vanilla");
        interpolationData.put(InterpolationConstants.LAUNCHER_VERSION, runtimeVersion == null ? "DEV" : runtimeVersion);
    }

    @Override
    public void configureRun(RunImpl run) {
        if (getSpecification().getDistribution().isClient()) {
            run.getArguments().addAll(
                    getVersionJson().map(VersionJson::getArguments)
                            .map(VersionJson.Arguments::getGame)
                            .map(Arrays::stream)
                            .map(stream -> stream
                                    .filter(VersionJson.RuledObject::isAllowed)
                                    .flatMap(arg -> arg.value.stream())
                                    .toList()
                            )

            );
            run.getJvmArguments().addAll(
                    getVersionJson().map(VersionJson::getArguments)
                            .map(VersionJson.Arguments::getJvm)
                            .map(Arrays::stream)
                            .map(stream -> stream
                                    .filter(VersionJson.RuledObject::isAllowed)
                                    .flatMap(arg -> arg.value.stream())
                                    .toList()
                            )
            );
            run.getMainClass().set(getVersionJson().map(VersionJson::getMainClass));
            run.getIsClient().set(true);
            run.getIsSingleInstance().set(false);
            
            final MapProperty<String, String> interpolationData = run.getProject().getObjects().mapProperty(String.class, String.class);
            buildRunInterpolationData(run, interpolationData);

            interpolationData.put(InterpolationConstants.GAME_DIRECTORY, run.getWorkingDirectory().get().getAsFile().getAbsolutePath());
            run.overrideJvmArguments(interpolate(run.getJvmArguments(), interpolationData, "$"));
            run.overrideArguments(interpolate(run.getArguments(), interpolationData, "$"));
            run.overrideEnvironmentVariables(interpolate(run.getEnvironmentVariables(), interpolationData, "$"));
            run.overrideSystemProperties(interpolate(run.getSystemProperties(), interpolationData, "$"));
        } else if (getSpecification().getDistribution().isServer()) {
            final ServerLaunchInformation launchInformation = getServerLaunchInformation().orElseThrow(() -> new IllegalStateException("Server launch information not present for server distribution"));
            run.getMainClass().set(launchInformation.getMainClass());
            run.getIsClient().set(false);
            run.getIsSingleInstance().set(true);
        }
    }
}
