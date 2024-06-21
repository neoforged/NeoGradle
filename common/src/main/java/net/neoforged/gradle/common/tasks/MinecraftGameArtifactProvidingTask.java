package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import org.gradle.api.provider.Provider;

public interface MinecraftGameArtifactProvidingTask extends WithOutput {

    GameArtifact gameArtifact();
}
