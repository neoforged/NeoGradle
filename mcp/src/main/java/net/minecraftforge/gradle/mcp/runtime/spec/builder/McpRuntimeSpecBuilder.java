package net.minecraftforge.gradle.mcp.runtime.spec.builder;

import net.minecraftforge.gradle.mcp.runtime.tasks.McpRuntimeTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Optional;
import java.util.function.Function;

public final class McpRuntimeSpecBuilder {

    private final Project project;
    private String namePrefix = "";
    private String mcpVersion;
    private String side;

    private Function<Provider<File>, Optional<McpRuntimeTask>> preDecompileTaskTreeModifier = property -> Optional.empty();

    private McpRuntimeSpecBuilder(Project project) {
        this.project = project;
    }

    public static McpRuntimeSpecBuilder from(final Project project) {
        return new McpRuntimeSpecBuilder(project);
    }

    public McpRuntimeSpecBuilder withNamePrefix(final String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    public McpRuntimeSpecBuilder withMcpVersion(String mcpVersion) {
        this.mcpVersion = mcpVersion;
        return this;
    }

    public McpRuntimeSpecBuilder withSide(String side) {
        this.side = side;
        return this;
    }

    public McpRuntimeSpecBuilder withPreDecompileTaskTreeModifier(Function<Provider<File>, Optional<McpRuntimeTask>> preDecompileTaskTreeModifier) {
        this.preDecompileTaskTreeModifier = preDecompileTaskTreeModifier;
        return this;
    }

    public McpRuntimeSpec build() {
        return new McpRuntimeSpec(project, namePrefix, mcpVersion, side, preDecompileTaskTreeModifier);
    }
}
