package net.neoforged.gradle.platform.runtime.definition;

import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.platform.runtime.specification.PlatformDevRuntimeSpecification;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Represents a configured and registered runtime for forges platform development environment.
 */
//TODO: Create DSL for platform
public final class PlatformDevRuntimeDefinition extends CommonRuntimeDefinition<PlatformDevRuntimeSpecification> implements IDelegatingRuntimeDefinition<PlatformDevRuntimeSpecification> {
    private final NeoFormRuntimeDefinition joinedNeoFormRuntimeDefinition;
    
    public PlatformDevRuntimeDefinition(@NotNull PlatformDevRuntimeSpecification specification, NeoFormRuntimeDefinition joinedNeoFormRuntimeDefinition, TaskProvider<? extends ArtifactProvider> sourcesProvider) {
        super(specification, joinedNeoFormRuntimeDefinition.getTasks(), sourcesProvider, joinedNeoFormRuntimeDefinition.getRawJarTask(), joinedNeoFormRuntimeDefinition.getGameArtifactProvidingTasks(), joinedNeoFormRuntimeDefinition.getMinecraftDependenciesConfiguration(), joinedNeoFormRuntimeDefinition::configureAssociatedTask);
        this.joinedNeoFormRuntimeDefinition = joinedNeoFormRuntimeDefinition;
    }
    
    public NeoFormRuntimeDefinition getJoinedNeoFormRuntimeDefinition() {
        return joinedNeoFormRuntimeDefinition;
    }
    
    @Override
    public void setReplacedDependency(@NotNull Dependency dependency) {
        super.setReplacedDependency(dependency);
        joinedNeoFormRuntimeDefinition.setReplacedDependency(dependency);
    }


    @Override
    public void onRepoWritten(@NotNull final TaskProvider<? extends WithOutput> finalRepoWritingTask) {
        joinedNeoFormRuntimeDefinition.onRepoWritten(finalRepoWritingTask);
    }

    @Override
    public @NotNull TaskProvider<DownloadAssets> getAssetsTaskProvider() {
        return joinedNeoFormRuntimeDefinition.getAssetsTaskProvider();
    }

    @Override
    public @NotNull TaskProvider<ExtractNatives> getNativesTaskProvider() {
        return joinedNeoFormRuntimeDefinition.getNativesTaskProvider();
    }

    public @NotNull TaskProvider<? extends WithOutput> getDebuggingMappingsTaskProvider() {
        return joinedNeoFormRuntimeDefinition.getDebuggingMappingsTaskProvider();
    }

    @Override
    public @NotNull Map<String, String> getMappingVersionData() {
        return joinedNeoFormRuntimeDefinition.getMappingVersionData();
    }

    @Override
    public void configureRun(RunImpl run) {
        super.configureRun(run);
        run.getClasspath().from(getDebuggingMappingsTaskProvider());
    }

    @NotNull
    @Override
    public TaskProvider<? extends WithOutput> getListLibrariesTaskProvider() {
        return joinedNeoFormRuntimeDefinition.getListLibrariesTaskProvider();
    }

    @Override
    protected Map<String, String> buildRunInterpolationData() {
        final Map<String, String> interpolationData = joinedNeoFormRuntimeDefinition.buildRunInterpolationData();
        return interpolationData;
    }

    @Override
    public Definition<?> getDelegate() {
        return joinedNeoFormRuntimeDefinition;
    }
}
