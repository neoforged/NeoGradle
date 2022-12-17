package net.minecraftforge.gradle.common.runtime;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.common.runtime.tasks.ArtifactProvider;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.GameArtifact;
import org.apache.commons.lang3.NotImplementedException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.impldep.org.eclipse.jgit.errors.NotSupportedException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a configured and registered runtime for Mcp.
 */
public abstract class CommonRuntimeDefinition<S extends CommonRuntimeSpec> {
    private final S spec;
    private final LinkedHashMap<String, TaskProvider<? extends ITaskWithOutput>> taskOutputs;
    private final TaskProvider<? extends ArtifactProvider> sourceJarTask;
    private final TaskProvider<? extends ArtifactProvider> rawJarTask;
    private final Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactProvidingTasks;
    private final Map<GameArtifact, File> gameArtifacts;
    private final Configuration minecraftDependenciesConfiguration;
    private final Map<String, String> configuredMappingVersionData = Maps.newHashMap();
    private Dependency replacedDependency = null;

    protected CommonRuntimeDefinition(
            S spec,
            LinkedHashMap<String, TaskProvider<? extends ITaskWithOutput>> taskOutputs,
            TaskProvider<? extends ArtifactProvider> sourceJarTask,
            TaskProvider<? extends ArtifactProvider> rawJarTask,
            Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactProvidingTasks,
            Map<GameArtifact, File> gameArtifacts,
            Configuration minecraftDependenciesConfiguration) {
        this.spec = spec;
        this.taskOutputs = taskOutputs;
        this.sourceJarTask = sourceJarTask;
        this.rawJarTask = rawJarTask;
        this.gameArtifactProvidingTasks = gameArtifactProvidingTasks;
        this.gameArtifacts = gameArtifacts;
        this.minecraftDependenciesConfiguration = minecraftDependenciesConfiguration;
    }

    /**
     * Returns the runtimes task with the given name.
     * The given name is prefixed with the name of the runtime, if needed.
     * Invoking this method with the name of a task that is not part of the runtime will result in an {@link IllegalArgumentException exception}.
     *
     * @param name The name of the task to get.
     * @return The named task.
     */
    @NotNull
    public final Provider<? extends ITaskWithOutput> task(String name) {
        final String taskName = CommonRuntimeUtils.buildTaskName(this, name);
        if (!taskOutputs.containsKey(taskName)) {
            throw new IllegalArgumentException("No task with name " + name + " found in runtime " + spec.name());
        }

        return taskOutputs.get(taskName);
    }

    /**
     * Returns the task which produces the raw jar used for compiling against.
     *
     * @return The raw jar producing taskOutputs.
     */
    public final TaskProvider<? extends ArtifactProvider> rawJarTask() {
        return rawJarTask;
    }

    public final S spec() {
        return spec;
    }

    public final LinkedHashMap<String, TaskProvider<? extends ITaskWithOutput>> taskOutputs() {
        return taskOutputs;
    }

    public final TaskProvider<? extends ArtifactProvider> sourceJarTask() {
        return sourceJarTask;
    }

    public final Map<GameArtifact, TaskProvider<? extends ITaskWithOutput>> gameArtifactProvidingTasks() {
        return gameArtifactProvidingTasks;
    }

    public final Map<GameArtifact, File> gameArtifacts() {
        return gameArtifacts;
    }

    public final Configuration minecraftDependenciesConfiguration() {
        return minecraftDependenciesConfiguration;
    }

    public Dependency replacedDependency() {
        if (this.replacedDependency == null)
            throw new IllegalStateException("No dependency has been replaced yet.");

        return this.replacedDependency;
    }

    public void replacedDependency(Dependency dependency) {
        this.replacedDependency = dependency;
    }

    public Map<String, String> configuredMappingVersionData() {
        return configuredMappingVersionData;
    }

    public void configureMappingVersionData(Map<String, String> data) {
        configuredMappingVersionData.clear();
        configuredMappingVersionData.putAll(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommonRuntimeDefinition)) return false;

        CommonRuntimeDefinition<?> that = (CommonRuntimeDefinition<?>) o;

        if (!spec.equals(that.spec)) return false;
        if (!taskOutputs.equals(that.taskOutputs)) return false;
        if (!sourceJarTask.equals(that.sourceJarTask)) return false;
        if (!rawJarTask.equals(that.rawJarTask)) return false;
        if (!gameArtifactProvidingTasks.equals(that.gameArtifactProvidingTasks)) return false;
        return minecraftDependenciesConfiguration.equals(that.minecraftDependenciesConfiguration);
    }

    @Override
    public int hashCode() {
        int result = spec.hashCode();
        result = 31 * result + taskOutputs.hashCode();
        result = 31 * result + sourceJarTask.hashCode();
        result = 31 * result + rawJarTask.hashCode();
        result = 31 * result + gameArtifactProvidingTasks.hashCode();
        result = 31 * result + minecraftDependenciesConfiguration.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CommonRuntimeDefinition{" +
                "spec=" + spec +
                ", taskOutputs=" + taskOutputs +
                ", sourceJarTask=" + sourceJarTask +
                ", rawJarTask=" + rawJarTask +
                ", gameArtifactProvidingTasks=" + gameArtifactProvidingTasks +
                ", minecraftDependenciesConfiguration=" + minecraftDependenciesConfiguration +
                '}';
    }
}
