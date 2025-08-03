package net.neoforged.gradle.vanilla.dependency;

import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementAware;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementResult;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.vanilla.runtime.VanillaRuntimeDefinition;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class VanillaReplacementResult extends ReplacementResult implements ReplacementAware
{

    private final VanillaRuntimeDefinition definition;

    public VanillaReplacementResult(
        final Project project,
        @Nullable final TaskProvider<? extends WithOutput> sourcesJar,
        final TaskProvider<? extends WithOutput> rawJar,
        final Configuration sdk,
        final Configuration dependencies,
        final Set<TaskProvider<? extends Task>> additionalTasks,
        final VanillaRuntimeDefinition definition)
    {
        super(project, sourcesJar, rawJar, sdk, dependencies, additionalTasks);
        this.definition = definition;
    }

    @Override
    public void onTasksCreated(final TaskProvider<? extends WithOutput> copiesRawJar, final TaskProvider<? extends WithOutput> copiesMappedJar)
    {
        //Noop
    }

    @Override
    public ExternalModuleDependency getReplacementDependency(final ExternalModuleDependency externalModuleDependency)
    {
        final Dependency resolvedExactVersionDependency = getProject().getDependencies()
            .create(
                externalModuleDependency.getGroup() + ":" + externalModuleDependency.getName() + ":" + definition.getSpecification().getMinecraftVersion()
            );

        if (!(resolvedExactVersionDependency instanceof ExternalModuleDependency))
            throw new IllegalStateException("Resolved dependency is not an ExternalModuleDependency");

        return (ExternalModuleDependency) resolvedExactVersionDependency;
    }
}
