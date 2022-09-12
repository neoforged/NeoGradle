package net.minecraftforge.gradle.mcp.runtime.spec.builder;

import net.minecraftforge.gradle.mcp.runtime.tasks.McpRuntimeTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Optional;
import java.util.function.Function;

public record McpRuntimeSpec(Project project, String namePrefix, String mcpVersion, String side, Function<Provider<File>, Optional<McpRuntimeTask>> preDecompileTaskTreeModifier) {
}
