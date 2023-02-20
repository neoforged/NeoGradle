package net.minecraftforge.gradle.base.extensions;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

public class ProjectEvaluationExtensionTest {

    @Test
    void getProjectReturnsTheProjectPassedInDuringConstruction() {
        Project mockProject = mock(Project.class);
        doNothing().when(mockProject).afterEvaluate(ArgumentMatchers.<Action<? super Project>>any());
        doNothing().when(mockProject).afterEvaluate(ArgumentMatchers.<Closure<?>>any());

        ProjectEvaluationExtension projectEvaluationExtension = new ProjectEvaluationExtension(mockProject) {
        };

        assertEquals(mockProject, projectEvaluationExtension.getProject());
    }

    @Test
    void constructingAnInstanceRegistersAnAfterEvalListener() {
        final List<Object> listenerList = new ArrayList<>();
        Project mockProject = mock(Project.class);

        doAnswer(invocation -> {
            listenerList.add(invocation.getArgument(0));
            return null;
        }).when(mockProject).afterEvaluate(ArgumentMatchers.<Action<? super Project>>any());
        doAnswer(invocation -> {
            listenerList.add(invocation.getArgument(0));
            return null;
        }).when(mockProject).afterEvaluate(ArgumentMatchers.<Closure<?>>any());

        new ProjectEvaluationExtension(mockProject) {
        };

        assertEquals(1, listenerList.size());
    }

    @Test
    void invokingAfterEvaluateWhenInAnPostEvalStateRunsImmediately(){
        final Project mockProject = mock(Project.class);

        //When the project after eval is called, immediately invoke the callback since we are simulating an after eval phase
        doAnswer(invocation -> {
            final Action<Project> action = invocation.getArgument(0);
            action.execute(mockProject);
            return null;
        }).when(mockProject).afterEvaluate(ArgumentMatchers.<Action<? super Project>>any());
        doAnswer(invocation -> {
            final Closure<?> closure = invocation.getArgument(0);
            closure.call(mockProject);
            return null;
        }).when(mockProject).afterEvaluate(ArgumentMatchers.<Closure<?>>any());

        final ProjectEvaluationExtension extension = new ProjectEvaluationExtension(mockProject) {
        };

        final AtomicBoolean called = new AtomicBoolean(false);
        extension.afterEvaluate(() -> called.set(true));

        assertTrue(called.get());
    }

    @Test
    void invokingAfterEvaluateWhenInAnPreEvalStateRunsDelayed(){
        List<Runnable> listeners = new ArrayList<>();
        final Project mockProject = mock(Project.class);

        //When the project after eval is called, immediately invoke the callback since we are simulating an after eval phase
        doAnswer(invocation -> {
            listeners.add(() -> {
                final Action<Project> action = invocation.getArgument(0);
                action.execute(mockProject);
            });

            return null;
        }).when(mockProject).afterEvaluate(ArgumentMatchers.<Action<? super Project>>any());
        doAnswer(invocation -> {
            listeners.add(() -> {
                final Closure<?> closure = invocation.getArgument(0);
                closure.call(mockProject);
            });

            return null;
        }).when(mockProject).afterEvaluate(ArgumentMatchers.<Closure<?>>any());

        final ProjectEvaluationExtension extension = new ProjectEvaluationExtension(mockProject) {
        };

        final AtomicBoolean called = new AtomicBoolean(false);
        extension.afterEvaluate(() -> called.set(true));

        assertFalse(called.get());

        listeners.forEach(Runnable::run);

        assertTrue(called.get());
    }
}