package net.minecraftforge.gradle.base.tasks;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.Nullable;
import org.mockito.ArgumentMatchers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class TaskMockingUtils {

    private TaskMockingUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: TaskMockingUtils. This is a utility class");
    }

    public static <T extends Task> T mockTask(Class<T> taskClass, final Project project, final String name) {
        final T task = mock(taskClass);

        when(task.getProject()).thenReturn(project);
        when(task.getName()).thenReturn(name);

        final List<Method> properties = Arrays.stream(taskClass.getMethods())
                .filter(method -> method.getName().startsWith("get") && method.getParameterCount() == 0)
                .filter(method -> method.getReturnType() == Property.class)
                .collect(Collectors.toList());

        for (Method property : properties) {
            AtomicReference<Supplier<Object>> value = new AtomicReference<>();
            final Property<Object> mockProperty = mock(Property.class);
            when(mockProperty.get()).thenAnswer(invocation -> value.get().get());
            doAnswer(invocation -> {
                value.set(() -> invocation.getArgument(0));
                return null;
            }).when(mockProperty).set(ArgumentMatchers.<Object>any());
            doAnswer(invocation -> {
                final Provider<?> provider = invocation.getArgument(0);
                value.set(provider::get);
                return null;
            }).when(mockProperty).set(ArgumentMatchers.any());

            try {
                when(property.invoke(task)).thenReturn(mockProperty);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Can not mock property!", e);
            }
        }

        final List<Method> fileProperties = Arrays.stream(taskClass.getMethods())
                .filter(method -> method.getName().startsWith("get") && method.getParameterCount() == 0)
                .filter(method -> method.getReturnType() == RegularFileProperty.class)
                .collect(Collectors.toList());

        for (Method property : fileProperties) {
            AtomicReference<Supplier<RegularFile>> value = new AtomicReference<>();
            final RegularFileProperty mockProperty = mock(RegularFileProperty.class);
            when(mockProperty.get()).thenAnswer(invocation -> value.get().get());
            doAnswer(invocation -> {
                final RegularFile regularFile = invocation.getArgument(0);
                value.set(() -> regularFile);
                return null;
            }).when(mockProperty).set(ArgumentMatchers.<File>any());
            doAnswer(invocation -> {
                final File file = invocation.getArgument(0);
                final RegularFile regularFile = mock(RegularFile.class);
                when(regularFile.getAsFile()).thenReturn(file);
                value.set(() -> regularFile);
                return null;
            }).when(mockProperty).set(ArgumentMatchers.<File>any());
            doAnswer(invocation -> {
                final Provider<? extends RegularFile> provider = invocation.getArgument(0);
                value.set(provider::get);
                return null;
            }).when(mockProperty).set(ArgumentMatchers.<Provider<? extends RegularFile>>any());

            try {
                when(property.invoke(task)).thenReturn(mockProperty);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Can not mock property!", e);
            }
        }

        //TODO: DirectoryProperty
        //TODO: ListProperty
        //TODO: MapProperty

        return task;
    }

    public static <T extends Task> TaskProvider<T> mockTaskProvider(final T task) {
        final TaskProvider<T> taskProvider = mock(TaskProvider.class);
        when(taskProvider.get()).thenReturn(task);
        when(taskProvider.getName()).thenAnswer(invocation -> task.getName());
        when(taskProvider.getOrNull()).thenReturn(task);
        when(taskProvider.getOrElse(ArgumentMatchers.any())).thenReturn(task);
        when(taskProvider.map(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Transformer<? extends Task, T> transformer = invocation.getArgument(0);
            final Task resultTask = transformer.transform(task);
            return mockTaskProvider(resultTask);
        });
        when(taskProvider.flatMap(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Transformer<? extends Provider<? extends Task>, T> transformer = invocation.getArgument(0);
            return transformer.transform(task);
        });
        doAnswer(invocation -> {
            final Action<? super TaskProvider<T>> action = invocation.getArgument(0);
            action.execute(taskProvider);
            return null;
        }).when(taskProvider).configure(ArgumentMatchers.any());

        return taskProvider;
    }
}
