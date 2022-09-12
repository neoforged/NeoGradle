package net.minecraftforge.gradle.mcp.runtime;

import net.minecraftforge.gradle.mcp.runtime.spec.builder.McpRuntimeSpec;
import net.minecraftforge.gradle.mcp.runtime.tasks.McpRuntimeTask;
import net.minecraftforge.gradle.mcp.tasks.RunMcp;
import org.gradle.api.tasks.Input;

import java.util.LinkedHashMap;

public record McpRuntime(@Input McpRuntimeSpec spec, @Input LinkedHashMap<String, McpRuntimeTask> tasks, @Input RunMcp runMcp) {
}
