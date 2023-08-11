package net.neoforged.gradle.neoform.runtime.definition;

import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV2;
import net.neoforged.gradle.dsl.neoform.runtime.definition.NeoFormDefinition;
import net.neoforged.gradle.neoform.runtime.specification.NeoFormRuntimeSpecification;
import net.neoforged.gradle.common.runs.run.RunImpl;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a configured and registered runtime for Mcp.
 */
public class NeoFormRuntimeDefinition extends CommonRuntimeDefinition<NeoFormRuntimeSpecification> implements NeoFormDefinition<NeoFormRuntimeSpecification> {
    private final File unpackedneoformZipDirectory;
    private final NeoFormConfigConfigurationSpecV2 neoform;

    private final TaskProvider<DownloadAssets> assetsTaskProvider;
    private final TaskProvider<ExtractNatives> nativesTaskProvider;
    private TaskProvider<? extends WithOutput> debuggingMappingsTaskProvider;

    public NeoFormRuntimeDefinition(@NotNull NeoFormRuntimeSpecification specification,
                                    @NotNull LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs,
                                    @NotNull TaskProvider<? extends ArtifactProvider> sourceJarTask,
                                    @NotNull TaskProvider<? extends ArtifactProvider> rawJarTask,
                                    @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactProvidingTasks,
                                    @NotNull Configuration minecraftDependenciesConfiguration,
                                    @NotNull Consumer<TaskProvider<? extends Runtime>> associatedTaskConsumer,
                                    @NotNull File unpackedneoformZipDirectory,
                                    @NotNull NeoFormConfigConfigurationSpecV2 neoform,
                                    @NotNull TaskProvider<DownloadAssets> assetsTaskProvider,
                                    @NotNull TaskProvider<ExtractNatives> nativesTaskProvider) {
        super(specification, taskOutputs, sourceJarTask, rawJarTask, gameArtifactProvidingTasks, minecraftDependenciesConfiguration, associatedTaskConsumer);
        this.unpackedneoformZipDirectory = unpackedneoformZipDirectory;
        this.neoform = neoform;
        this.assetsTaskProvider = assetsTaskProvider;
        this.nativesTaskProvider = nativesTaskProvider;
    }


    @Override
    @NotNull
    public File getUnpackedNeoFormZipDirectory() {
        return unpackedneoformZipDirectory;
    }

    @Override
    @NotNull
    public NeoFormConfigConfigurationSpecV2 getNeoFormConfig() {
        return neoform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NeoFormRuntimeDefinition)) return false;
        if (!super.equals(o)) return false;

        NeoFormRuntimeDefinition that = (NeoFormRuntimeDefinition) o;

        if (!unpackedneoformZipDirectory.equals(that.unpackedneoformZipDirectory)) return false;
        return neoform.equals(that.neoform);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + unpackedneoformZipDirectory.hashCode();
        result = 31 * result + neoform.hashCode();
        return result;
    }

    @Override
    public @NotNull TaskProvider<DownloadAssets> getAssets() {
        return assetsTaskProvider;
    }

    @Override
    public @NotNull TaskProvider<ExtractNatives> getNatives() {
        return nativesTaskProvider;
    }

    public @NotNull TaskProvider<? extends WithOutput> getDebuggingMappingsTaskProvider() {
        return debuggingMappingsTaskProvider;
    }

    public void setDebuggingMappingsTaskProvider(TaskProvider<? extends WithOutput> debuggingMappingsTaskProvider) {
        this.debuggingMappingsTaskProvider = debuggingMappingsTaskProvider;
    }

    @Override
    public void configureRun(RunImpl run) {
        super.configureRun(run);
        run.getClasspath().from(this.getDebuggingMappingsTaskProvider());
    }

    @Override
    public Map<String, String> buildRunInterpolationData() {
        final Map<String, String> interpolationData = new HashMap<>(super.buildRunInterpolationData());

        interpolationData.put("mcp_version", neoform.getVersion());
        interpolationData.put("mcp_mappings", new File(unpackedneoformZipDirectory, "config/joined.srg").getAbsolutePath());

        return interpolationData;
    }

    @NotNull
    @Override
    public TaskProvider<? extends WithOutput> getListLibrariesTaskProvider() {
        return getTask("listLibraries");
    }
}
