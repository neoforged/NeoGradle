package net.minecraftforge.gradle.vanilla.runtime;

import net.minecraftforge.gradle.common.runtime.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.tasks.ArtifactProvider;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.GameArtifact;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a configured and registered runtime for vanilla.
 */
public final class VanillaRuntimeDefinition extends CommonRuntimeDefinition<VanillaRuntimeSpec> {

    public VanillaRuntimeDefinition(VanillaRuntimeSpec spec, LinkedHashMap<String, TaskProvider<? extends IRuntimeTask>> taskOutputs, TaskProvider<? extends ArtifactProvider> sourceJarTask, TaskProvider<? extends ArtifactProvider> rawJarTask, final Map<GameArtifact, File> gameArtifacts, final Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactProvidingTasks, Configuration minecraftDependenciesConfiguration) {
        super(spec, taskOutputs, sourceJarTask, rawJarTask, gameArtifactProvidingTasks, gameArtifacts, minecraftDependenciesConfiguration);
    }
}
