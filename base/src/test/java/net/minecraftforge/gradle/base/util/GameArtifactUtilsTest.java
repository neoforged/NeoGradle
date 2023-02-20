package net.minecraftforge.gradle.base.util;

import net.minecraftforge.gradle.dsl.base.util.GameArtifact;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameArtifactUtilsTest {

    @Test
    public void doWhenRequiredRunsWhenArtifactIsRequired() {
        final GameArtifact mockArtifact = mock(GameArtifact.class);
        when(mockArtifact.isRequiredForDistribution(any())).thenReturn(true);

        final AtomicBoolean ran = new AtomicBoolean(false);
        GameArtifactUtils.doWhenRequired(mockArtifact, null, () -> {
            ran.set(true);
        });

        Assertions.assertTrue(ran.get());
    }

    @Test
    public void doWhenRequiredDoesNotRunWhenArtifactIsNotRequired() {
        final GameArtifact mockArtifact = mock(GameArtifact.class);
        when(mockArtifact.isRequiredForDistribution(any())).thenReturn(false);

        final AtomicBoolean ran = new AtomicBoolean(false);
        GameArtifactUtils.doWhenRequired(mockArtifact, null, () -> {
            ran.set(true);
        });

        Assertions.assertFalse(ran.get());
    }
}