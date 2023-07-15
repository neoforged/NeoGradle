package net.neoforged.gradle.common.util;

import net.neoforged.gradle.util.GradleInternalUtils;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
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
        if (tasks.contains(task)) {
            if (queue.isEmpty()) {
                return;
            }

            getDependencies(queue, tasks);
            return;
        }

        ((Set<Task>) tasks).add(task);
        queue.addAll(task.getTaskDependencies().getDependencies(task));
        getDependencies(queue, tasks);
    }

    public static CommonRuntimeDefinition<?> realiseTaskAndExtractRuntimeDefinition(@NotNull Project project, TaskProvider<?> t) throws MultipleDefinitionsFoundException {
        return extractRuntimeDefinition(project, t.get());
    }

    @SuppressWarnings("unchecked")
    public static CommonRuntimeDefinition<?> extractRuntimeDefinition(@NotNull Project project, Task t) throws MultipleDefinitionsFoundException {
        final Optional<JavaCompile> javaCompile;
        if (t instanceof JavaCompile) {
            javaCompile = Optional.of((JavaCompile) t);
        } else {
            javaCompile = getDependencies(t).stream().filter(JavaCompile.class::isInstance).map(JavaCompile.class::cast)
                    .findFirst();
        }

        final Optional<List<? extends CommonRuntimeDefinition<?>>> listCandidate = javaCompile.map(JavaCompile::getClasspath)
                .filter(Configuration.class::isInstance)
                .map(Configuration.class::cast)
                .map(Configuration::getAllDependencies)
                .map(dependencies -> {
                    final CommonRuntimeExtension<?, ?, ? extends Definition<?>> runtimeExtension = project.getExtensions().getByType(CommonRuntimeExtension.class);
                    return runtimeExtension.getRuntimes().get()
                            .values()
                            .stream()
                            .filter(runtime -> dependencies.contains(runtime.getReplacedDependency()));
                })
                .map(stream -> stream.collect(Collectors.toList()));

        final List<? extends CommonRuntimeDefinition<?>> definitions = listCandidate.orElseThrow(() -> new IllegalStateException("Could not find runtime definition for task: " + t.getName()));
        final List<CommonRuntimeDefinition<?>> undelegated = unwrapDelegation(project, definitions);

        if (undelegated.size() != 1)
            throw new MultipleDefinitionsFoundException(undelegated);

        return undelegated.get(0);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private static List<CommonRuntimeDefinition<?>> unwrapDelegation(final Project project, final List<? extends CommonRuntimeDefinition<?>> input) {
        final List<CommonRuntimeDefinition<?>> output = new LinkedList<>();

        GradleInternalUtils.getExtensions(project.getExtensions())
                .stream()
                .filter(CommonRuntimeExtension.class::isInstance)
                .map(extension -> (CommonRuntimeExtension<?,?,?>) extension)
                .flatMap(extension -> extension.getRuntimes().get().values().stream())
                .filter(IDelegatingRuntimeDefinition.class::isInstance)
                .map(runtime -> (IDelegatingRuntimeDefinition<?>) runtime)
                .filter(runtime -> input.contains(runtime.getDelegate()))
                .map(runtime -> (CommonRuntimeDefinition<?>) runtime)
                .forEach(output::add);

        final List<CommonRuntimeDefinition<?>> noneDelegated = input.stream()
                .filter(runtime -> output.stream()
                        .filter(IDelegatingRuntimeDefinition.class::isInstance)
                        .map(r -> (IDelegatingRuntimeDefinition<?>) r)
                        .noneMatch(r -> r.getDelegate().equals(runtime))).collect(Collectors.toList());
        output.addAll(noneDelegated);
        return output.stream().distinct().collect(Collectors.toList());
    }
}
