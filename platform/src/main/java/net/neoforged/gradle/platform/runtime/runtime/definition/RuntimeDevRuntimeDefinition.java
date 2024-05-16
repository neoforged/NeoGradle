package net.neoforged.gradle.platform.runtime.runtime.definition;

import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.platform.runtime.runtime.specification.RuntimeDevRuntimeSpecification;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents a configured and registered runtime for forges runtime development environment.
 */
//TODO: Create DSL for runtime
public final class RuntimeDevRuntimeDefinition extends CommonRuntimeDefinition<RuntimeDevRuntimeSpecification> implements IDelegatingRuntimeDefinition<RuntimeDevRuntimeSpecification> {
    private final NeoFormRuntimeDefinition joinedNeoFormRuntimeDefinition;
    
    public RuntimeDevRuntimeDefinition(@NotNull RuntimeDevRuntimeSpecification specification, NeoFormRuntimeDefinition joinedNeoFormRuntimeDefinition, TaskProvider<? extends ArtifactProvider> sourcesProvider) {
        super(specification, joinedNeoFormRuntimeDefinition.getTasks(), sourcesProvider, joinedNeoFormRuntimeDefinition.getRawJarTask(), joinedNeoFormRuntimeDefinition.getGameArtifactProvidingTasks(), joinedNeoFormRuntimeDefinition.getMinecraftDependenciesConfiguration(), joinedNeoFormRuntimeDefinition::configureAssociatedTask, joinedNeoFormRuntimeDefinition.getVersionJson());
        this.joinedNeoFormRuntimeDefinition = joinedNeoFormRuntimeDefinition;
    }
    
    public NeoFormRuntimeDefinition getJoinedNeoFormRuntimeDefinition() {
        return joinedNeoFormRuntimeDefinition;
    }

    @Override
    public @NotNull TaskProvider<DownloadAssets> getAssets() {
        return joinedNeoFormRuntimeDefinition.getAssets();
    }

    @Override
    public @NotNull TaskProvider<ExtractNatives> getNatives() {
        return joinedNeoFormRuntimeDefinition.getNatives();
    }

    @Override
    public @NotNull Map<String, String> getMappingVersionData() {
        return joinedNeoFormRuntimeDefinition.getMappingVersionData();
    }

    @Override
    public void configureRun(RunImpl run) {
        super.configureRun(run);
    }

    @NotNull
    @Override
    public TaskProvider<? extends WithOutput> getListLibrariesTaskProvider() {
        return joinedNeoFormRuntimeDefinition.getListLibrariesTaskProvider();
    }

    @Override
    protected Map<String, String> buildRunInterpolationData(RunImpl run) {
        final Map<String, String> interpolationData = joinedNeoFormRuntimeDefinition.buildRunInterpolationData(run);
        return interpolationData;
    }

    @Override
    public Definition<?> getDelegate() {
        return joinedNeoFormRuntimeDefinition;
    }
}
