package net.neoforged.gradle.dsl.common.runtime.definition;

import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.runtime.spec.Specification;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import org.gradle.api.*;
import org.gradle.api.provider.Provider;

public record TaskHandler(
        NamedDomainObjectCollection<WithOutput> tasks,
        Specification specification,
        Action<Runtime> taskConfigurator
) {

    <T extends Runtime> NamedDomainObjectProvider<T> register(String name, Class<T> type, Action<? super T> configurationAction) throws InvalidUserDataException {
        final NamedDomainObjectProvider<T> taskProvider = specification.project().getTasks().register(name, type, task -> {
            taskConfigurator().execute(task);
            configurationAction.execute(task);
        });

        tasks.addLater(taskProvider);

        return taskProvider;
    }

    <T extends Runtime> NamedDomainObjectProvider<T> register(String name, Class<T> type) throws InvalidUserDataException {
        return register(name, type, t -> {});
    }

    public Provider<WithOutput> gameArtifactTask(GameArtifact artifact) {
        final MinecraftArtifactCache cache = specification.project().getExtensions().getByType(MinecraftArtifactCache.class);

        return specification.minecraftVersion()
                .flatMap(minecraftVersion -> cache.gameArtifactTask(tasks, artifact, minecraftVersion));
    }
}
