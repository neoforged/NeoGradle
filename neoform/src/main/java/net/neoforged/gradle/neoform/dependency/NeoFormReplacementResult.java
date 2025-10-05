package net.neoforged.gradle.neoform.dependency;

import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementAware;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementResult;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskProvider;

import java.util.Locale;
import java.util.Set;

/**
 * Special replacement result for NeoForm dependencies.
 * Is needed because NeoForm needs to know where the neoforge jar is, so it can put it on the classpathm
 * additionally we need to be notified when somebody registers us as a dependency and add the runtypes.
 */
public class NeoFormReplacementResult extends ReplacementResult implements ReplacementAware {

    private final NeoFormRuntimeDefinition definition;

    public NeoFormReplacementResult(Project project,
                                    TaskProvider<? extends WithOutput> sourcesJar,
                                    TaskProvider<? extends WithOutput> rawJar,
                                    Configuration sdk,
                                    Configuration dependencies,
                                    Set<TaskProvider<? extends Task>> additionalTasks,
                                    NeoFormRuntimeDefinition definition) {
        super(project, sourcesJar, rawJar, sdk, dependencies, additionalTasks);

        this.definition = definition;
    }

    @Override
    public void onTasksCreated(final TaskProvider<? extends WithOutput> copiesRawJar, final TaskProvider<? extends WithOutput> copiesMappedJar)
    {
        //Noop
    }

    @Override
    public ExternalModuleDependency getReplacementDependency(ExternalModuleDependency externalModuleDependency) {
        final Dependency resolvedExactVersionDependency = getProject().getDependencies()
                .create(
                        "net.minecraft:" + "neoform_" + definition.getSpecification().getDistribution().getName().toLowerCase(Locale.ROOT) + ":" + definition.getSpecification().getVersion()
                );

        if (!(resolvedExactVersionDependency instanceof ExternalModuleDependency))
            throw new IllegalStateException("Resolved dependency is not an ExternalModuleDependency");

        return (ExternalModuleDependency) resolvedExactVersionDependency;
    }
}
