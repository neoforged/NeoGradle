package net.minecraftforge.gradle.vanilla.runtime;

import net.minecraftforge.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.tasks.DownloadAssets;
import net.minecraftforge.gradle.common.runtime.tasks.ExtractNatives;
import net.minecraftforge.gradle.dsl.base.util.GameArtifact;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.ArtifactProvider;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpecification;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Represents a configured and registered runtime for vanilla.
 */
public final class VanillaRuntimeDefinition extends CommonRuntimeDefinition<VanillaRuntimeSpecification> {


    private final TaskProvider<DownloadAssets> assetsTaskProvider;
    private final TaskProvider<ExtractNatives> nativesTaskProvider;

    public VanillaRuntimeDefinition(@NotNull VanillaRuntimeSpecification specification, @NotNull LinkedHashMap<String, TaskProvider<? extends WithOutput>> taskOutputs, @NotNull TaskProvider<? extends ArtifactProvider> sourceJarTask, @NotNull TaskProvider<? extends ArtifactProvider> rawJarTask, @NotNull Map<GameArtifact, TaskProvider<? extends WithOutput>> gameArtifactProvidingTasks, @NotNull Configuration minecraftDependenciesConfiguration, @NotNull Consumer<TaskProvider<? extends Runtime>> associatedTaskConsumer, TaskProvider<DownloadAssets> assetsTaskProvider, TaskProvider<ExtractNatives> nativesTaskProvider) {
        super(specification, taskOutputs, sourceJarTask, rawJarTask, gameArtifactProvidingTasks, minecraftDependenciesConfiguration, associatedTaskConsumer);
        this.assetsTaskProvider = assetsTaskProvider;
        this.nativesTaskProvider = nativesTaskProvider;
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
}
