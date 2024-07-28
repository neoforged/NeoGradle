package net.neoforged.gradle.userdev.dependency;

import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementAware;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.ReplacementResult;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.tasks.TaskProvider;

import java.util.Set;

/**
 * Special replacement result for userdev dependencies.
 * Is needed because userdev needs to know where the neoforge jar is, so it can put it on the classpathm
 * additionally we need to be notified when somebody registers us as a dependency and add the runtypes.
 */
public class UserDevReplacementResult extends ReplacementResult implements ReplacementAware {

    private final UserDevRuntimeDefinition definition;

    public UserDevReplacementResult(Project project,
                                    TaskProvider<? extends WithOutput> sourcesJar,
                                    TaskProvider<? extends WithOutput> rawJar,
                                    Configuration sdk,
                                    Configuration dependencies,
                                    Set<TaskProvider<? extends Task>> additionalTasks,
                                    UserDevRuntimeDefinition definition) {
        super(project, sourcesJar, rawJar, sdk, dependencies, additionalTasks);

        this.definition = definition;
    }

    @Override
    public void onTasksCreated(TaskProvider<? extends WithOutput> copiesRawJar, TaskProvider<? extends WithOutput> copiesMappedJar) {
        //Register the classpath element producer
        definition.setUserdevClasspathElementProducer(copiesRawJar);
    }

    @Override
    public ExternalModuleDependency getReplacementDependency(ExternalModuleDependency externalModuleDependency) {
        final Dependency resolvedExactVersionDependency = getProject().getDependencies()
                .create(
                        definition.getSpecification().getForgeGroup() + ":" + definition.getSpecification().getForgeName() + ":" + definition.getSpecification().getForgeVersion()
                );

        if (!(resolvedExactVersionDependency instanceof ExternalModuleDependency))
            throw new IllegalStateException("Resolved dependency is not an ExternalModuleDependency");

        return (ExternalModuleDependency) resolvedExactVersionDependency;
    }
}
