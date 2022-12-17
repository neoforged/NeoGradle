package net.minecraftforge.gradle.common.util;

public final class GameArtifactUtils {

    private GameArtifactUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: GameArtifactUtils. This is a utility class");
    }

    public static void doWhenRequired(final GameArtifact artifact, final ArtifactSide side, final Runnable runnable) {
        if (artifact.isRequiredForSide(side)) {
            runnable.run();
        }
    }
}
