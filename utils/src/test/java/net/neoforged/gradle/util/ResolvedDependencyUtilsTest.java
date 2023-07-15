package net.neoforged.gradle.util;

import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedDependency;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResolvedDependencyUtilsTest {

    @Test
    public void returnsANullClassifierWhenNoArtifactsArePresent() {
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.emptySet());

        assertNull(ResolvedDependencyUtils.getClassifier(resolvedDependency));
    }

    @Test
    public void returnsANullClassifierWhenTheFirstArtifactHasNoClassifier() {
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        when(resolvedArtifact.getClassifier()).thenReturn(null);
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));

        assertNull(ResolvedDependencyUtils.getClassifier(resolvedDependency));
    }

    @Test
    public void returnsTheClassifierOfTheFirstArtifact() {
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        when(resolvedArtifact.getClassifier()).thenReturn("classifier");
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));

        assertEquals("classifier", ResolvedDependencyUtils.getClassifier(resolvedDependency));
    }

    @Test
    public void returnsANullExtensionWhenNoArtifactsArePresent() {
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.emptySet());

        assertNull(ResolvedDependencyUtils.getExtension(resolvedDependency));
    }

    @Test
    public void returnsANullExtensionWhenTheFirstArtifactHasNoExtension() {
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        when(resolvedArtifact.getExtension()).thenReturn(null);
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));

        assertNull(ResolvedDependencyUtils.getExtension(resolvedDependency));
    }

    @Test
    public void returnsTheExtensionOfTheFirstArtifact() {
        final ResolvedDependency resolvedDependency = mock(ResolvedDependency.class);
        final ResolvedArtifact resolvedArtifact = mock(ResolvedArtifact.class);
        when(resolvedArtifact.getExtension()).thenReturn("extension");
        when(resolvedDependency.getModuleArtifacts()).thenReturn(Collections.singleton(resolvedArtifact));

        assertEquals("extension", ResolvedDependencyUtils.getExtension(resolvedDependency));
    }
}