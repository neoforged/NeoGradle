package net.minecraftforge.gradle.common.util;

import net.minecraftforge.gradle.common.runtime.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.tasks.ArtifactProvider;
import net.minecraftforge.gradle.common.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import org.gradle.api.Buildable;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class TaskDependencyUtils {

    private TaskDependencyUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: TaskDependencyUtils. This is a utility class");
    }

    public static Set<? extends Task> getDependencies(Task task) {
        final LinkedHashSet<? extends Task> dependencies = new LinkedHashSet<>();
        final LinkedList<Task> queue = new LinkedList<>();
        queue.add(task);

        getDependencies(queue, dependencies);

        return dependencies;
    }

    public static Set<? extends Task> getDependencies(Buildable task) {
        final LinkedHashSet<? extends Task> dependencies = new LinkedHashSet<>();
        final LinkedList<Task> queue = new LinkedList<>(task.getBuildDependencies().getDependencies(null));

        getDependencies(queue, dependencies);

        return dependencies;
    }

    @SuppressWarnings("unchecked")
    private static void getDependencies(final LinkedList<Task> queue, final Set<? extends Task> tasks) {
        if (queue.isEmpty())
            return;

        final Task task = queue.removeFirst();
        if (tasks.contains(task))
            return;

        ((Set<Task>) tasks).add(task);
        queue.addAll(task.getTaskDependencies().getDependencies(task));
        getDependencies(queue, tasks);
    }

    public static CommonRuntimeDefinition<?> realiseTaskAndExtractRuntimeDefinition(@NotNull Project project, TaskProvider<?> t) throws MultipleDefinitionsFoundException {
        return extractRuntimeDefinition(project, t.get());
    }

    @SuppressWarnings("unchecked")
    public static CommonRuntimeDefinition<?> extractRuntimeDefinition(@NotNull Project project, Task t) throws MultipleDefinitionsFoundException {
        final Optional<List<? extends CommonRuntimeDefinition<?>>> listCandidate = t.getTaskDependencies().getDependencies(t).stream().filter(JavaCompile.class::isInstance).map(JavaCompile.class::cast)
                .findFirst()
                .map(JavaCompile::getClasspath)
                .filter(Configuration.class::isInstance)
                .map(Configuration.class::cast)
                .map(Configuration::getAllDependencies)
                .map(dependencies -> {
                    final CommonRuntimeExtension<?, ?, ? extends CommonRuntimeDefinition<?>> runtimeExtension = project.getExtensions().getByType(CommonRuntimeExtension.class);
                    return runtimeExtension.getRuntimes().get()
                            .values()
                            .stream()
                            .filter(runtime -> dependencies.contains(runtime.replacedDependency()));
                })
                .map(stream -> stream.collect(Collectors.toList()));

        final List<? extends CommonRuntimeDefinition<?>> definitions = listCandidate.orElseThrow(() -> new IllegalStateException("Could not find runtime definition for task: " + t.getName()));
        if (definitions.size() != 1)
            throw new MultipleDefinitionsFoundException(definitions);

        return definitions.get(0);
    }
}
