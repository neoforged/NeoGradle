package net.minecraftforge.gradle.common.util;

import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import net.minecraftforge.gradle.dsl.common.util.GameArtifact;

public final class GameArtifactUtils {

    private GameArtifactUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: GameArtifactUtils. This is a utility class");
    }

    public static void doWhenRequired(final GameArtifact artifact, final DistributionType side, final Runnable runnable) {
        if (artifact.isRequiredForSide(side)) {
            runnable.run();
        }
    }
}
