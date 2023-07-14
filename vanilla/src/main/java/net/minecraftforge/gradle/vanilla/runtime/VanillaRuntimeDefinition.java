package net.minecraftforge.gradle.vanilla.runtime;

import com.google.common.collect.Maps;
import net.minecraftforge.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.tasks.DownloadAssets;
import net.minecraftforge.gradle.common.runtime.tasks.ExtractNatives;
import net.minecraftforge.gradle.common.util.VersionJson;
import net.minecraftforge.gradle.dsl.common.runtime.naming.NamingChannel;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.ArtifactProvider;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.common.runs.run.RunImpl;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpecification;
import net.minecraftforge.gradle.vanilla.util.InterpolationConstants;
import net.minecraftforge.gradle.vanilla.util.ServerLaunchInformation;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private final VersionJson versionJson;
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
                                    VersionJson versionJson,
                                    TaskProvider<DownloadAssets> assetsTaskProvider,
                                    TaskProvider<ExtractNatives> nativesTaskProvider,
                                    Optional<ServerLaunchInformation> serverLaunchInformation) {
        super(specification, taskOutputs, sourceJarTask, rawJarTask, gameArtifactProvidingTasks, minecraftDependenciesConfiguration, associatedTaskConsumer);
        this.versionJson = versionJson;
        this.assetsTaskProvider = assetsTaskProvider;
        this.nativesTaskProvider = nativesTaskProvider;
        this.serverLaunchInformation = serverLaunchInformation;
    }

    @Override
    @NotNull
    public TaskProvider<DownloadAssets> getAssetsTaskProvider() {
        return assetsTaskProvider;
    }

    @Override
    @NotNull
    public TaskProvider<ExtractNatives> getNativesTaskProvider() {
        return nativesTaskProvider;
    }

    @Override
    @NotNull
    public TaskProvider<? extends WithOutput> getListLibrariesTaskProvider() {
        return this.getTask("libraries");
    }

    public VersionJson getVersionJson() {
        return versionJson;
    }

    public Optional<ServerLaunchInformation> getServerLaunchInformation() {
        return serverLaunchInformation;
    }

    @Override
    protected Map<String, String> buildRunInterpolationData() {
        final Map<String, String> interpolationData = Maps.newHashMap();

        final String fgVersion = this.getClass().getPackage().getImplementationVersion();

        interpolationData.put(InterpolationConstants.VERSION_NAME, getSpecification().getMinecraftVersion());
        interpolationData.put(InterpolationConstants.ASSETS_ROOT, getAssetsTaskProvider().get().getOutputDirectory().get().getAsFile().getAbsolutePath());
        interpolationData.put(InterpolationConstants.ASSETS_INDEX_NAME, getAssetsTaskProvider().get().getAssetIndexFile().get().getAsFile().getName().substring(0, getAssetsTaskProvider().get().getAssetIndexFile().get().getAsFile().getName().lastIndexOf('.')));
        interpolationData.put(InterpolationConstants.AUTH_ACCESS_TOKEN, "0");
        interpolationData.put(InterpolationConstants.USER_TYPE, "legacy");
        interpolationData.put(InterpolationConstants.VERSION_TYPE, getVersionJson().getType());
        interpolationData.put(InterpolationConstants.NATIVES_DIRECTORY, getNativesTaskProvider().get().getOutputDirectory().get().getAsFile().getAbsolutePath());
        interpolationData.put(InterpolationConstants.LAUNCHER_NAME, "ForgeGradle-Vanilla");
        interpolationData.put(InterpolationConstants.LAUNCHER_VERSION, fgVersion == null ? "DEV" : fgVersion);

        return interpolationData;
    }

    @Override
    public void configureRun(RunImpl run) {


        if (getSpecification().getDistribution().isClient()) {
            Arrays.stream(getVersionJson().getArguments().getGame()).filter(arg -> arg.getRules() == null || arg.getRules().length == 0).flatMap(arg -> arg.value.stream()).forEach(arg -> run.getProgramArguments().add(arg));
            Arrays.stream(getVersionJson().getArguments().getJvm()).filter(VersionJson.RuledObject::isAllowed).flatMap(arg -> arg.value.stream()).forEach(arg -> run.getJvmArguments().add(arg));
            run.getMainClass().set(getVersionJson().getMainClass());
            run.getIsClient().set(true);
            run.getIsSingleInstance().set(false);
            
            final Map<String, String> interpolationData = Maps.newHashMap(buildRunInterpolationData());

            interpolationData.put(InterpolationConstants.GAME_DIRECTORY, run.getWorkingDirectory().get().getAsFile().getAbsolutePath());
            run.overrideJvmArguments(interpolate(run.getJvmArguments(), interpolationData, "$"));
            run.overrideProgramArguments(interpolate(run.getProgramArguments(), interpolationData, "$"));
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
