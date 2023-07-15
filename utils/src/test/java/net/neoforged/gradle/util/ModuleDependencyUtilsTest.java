package net.neoforged.gradle.util;

import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ModuleDependency;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModuleDependencyUtilsTest {

    @Test
    public void returnsANullClassifierWhenNoArtifactsArePresent() {
        final ModuleDependency moduleDependency = mock(ModuleDependency.class);
        when(moduleDependency.getArtifacts()).thenReturn(Collections.emptySet());

        assertNull(ModuleDependencyUtils.getClassifier(moduleDependency));
    }

    @Test
    public void returnsANullClassifierWhenTheFirstArtifactHasNoClassifier() {
        final ModuleDependency moduleDependency = mock(ModuleDependency.class);
        final DependencyArtifact dependencyArtifact = mock(DependencyArtifact.class);
        when(dependencyArtifact.getClassifier()).thenReturn(null);
        when(moduleDependency.getArtifacts()).thenReturn(Collections.singleton(dependencyArtifact));

        assertNull(ModuleDependencyUtils.getClassifier(moduleDependency));
    }

    @Test
    public void returnsTheClassifierOfTheFirstArtifact() {
        final ModuleDependency moduleDependency = mock(ModuleDependency.class);
        final DependencyArtifact dependencyArtifact = mock(DependencyArtifact.class);
        when(dependencyArtifact.getClassifier()).thenReturn("classifier");
        when(moduleDependency.getArtifacts()).thenReturn(Collections.singleton(dependencyArtifact));

        assertEquals("classifier", ModuleDependencyUtils.getClassifier(moduleDependency));
    }

    @Test
    public void returnsANullExtensionWhenNoArtifactsArePresent() {
        final ModuleDependency moduleDependency = mock(ModuleDependency.class);
        when(moduleDependency.getArtifacts()).thenReturn(Collections.emptySet());

        assertNull(ModuleDependencyUtils.getExtension(moduleDependency));
    }

    @Test
    public void returnsANullExtensionWhenTheFirstArtifactHasNoExtension() {
        final ModuleDependency moduleDependency = mock(ModuleDependency.class);
        final DependencyArtifact dependencyArtifact = mock(DependencyArtifact.class);
        when(dependencyArtifact.getExtension()).thenReturn(null);
        when(moduleDependency.getArtifacts()).thenReturn(Collections.singleton(dependencyArtifact));

        assertNull(ModuleDependencyUtils.getExtension(moduleDependency));
    }

    @Test
    public void returnsTheExtensionOfTheFirstArtifact() {
        final ModuleDependency moduleDependency = mock(ModuleDependency.class);
        final DependencyArtifact dependencyArtifact = mock(DependencyArtifact.class);
        when(dependencyArtifact.getExtension()).thenReturn("extension");
        when(moduleDependency.getArtifacts()).thenReturn(Collections.singleton(dependencyArtifact));

        assertEquals("extension", ModuleDependencyUtils.getExtension(moduleDependency));
    }
}