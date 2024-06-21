package net.neoforged.gradle.dsl.common.runtime.definition;

import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import org.gradle.api.*;

public record TaskHandler(
        Project project,
        NamedDomainObjectCollection<Runtime> tasks,
        Action<Runtime> taskConfigurator
) {
    public TaskHandler(Project project, Action<Runtime> taskConfigurator) {
        this(project, project.getObjects().namedDomainObjectList(Runtime.class), taskConfigurator);
    }

    public <T extends Runtime> NamedDomainObjectProvider<T> register(String name, Class<T> type, Action<? super T> configurationAction) throws InvalidUserDataException {
        final NamedDomainObjectProvider<T> taskProvider = project.getTasks().register(name, type, task -> {
            taskConfigurator().execute(task);
            configurationAction.execute(task);
        });

        tasks().addLater(taskProvider);

        return taskProvider;
    }

    public <T extends Runtime> NamedDomainObjectProvider<T> register(String name, Class<T> type) throws InvalidUserDataException {
        return register(name, type, t -> {});
    }

    public NamedDomainObjectProvider<? extends WithOutput> task(GameArtifact artifact) {
        final MinecraftArtifactCache cache = project.getExtensions().getByType(MinecraftArtifactCache.class);
        //return cache.gameArtifactTask(tasks, artifact);
        return null;
    }
}
