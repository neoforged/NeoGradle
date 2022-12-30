package net.minecraftforge.gradle.mcp.naming;

import net.minecraftforge.gradle.common.runtime.naming.renamer.IMappingFileSourceRenamer;
import net.minecraftforge.gradle.common.runtime.naming.renamer.IMappingFileTypeRenamer;
import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyMappingsToSourceJar;
import net.minecraftforge.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToCompiledJar;
import net.minecraftforge.gradle.common.runtime.naming.tasks.UnapplyOfficialMappingsToAccessTransformer;
import net.minecraftforge.gradle.common.runtime.naming.tasks.UnapplyOfficialMappingsToCompiledJar;
import net.minecraftforge.gradle.common.util.IMappingFileUtils;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.runtime.naming.NamingChannel;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.mcp.naming.tasks.WriteIMappingsFile;
import net.minecraftforge.gradle.mcp.runtime.definition.McpRuntimeDefinition;
import net.minecraftforge.gradle.mcp.util.CacheableIMappingFile;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

public final class MCPOfficialNamingChannelConfigurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MCPOfficialNamingChannelConfigurator.class);
    private static final MCPOfficialNamingChannelConfigurator INSTANCE = new MCPOfficialNamingChannelConfigurator();

    private MCPOfficialNamingChannelConfigurator() {
    }

    public static MCPOfficialNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    public void configure(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);

        final NamingChannel namingChannel = minecraftExtension.getNamingChannelProviders().getByName("official");
        minecraftExtension.getNamingChannelProviders().remove(namingChannel);

        minecraftExtension.getNamingChannelProviders().register("official", newOfficialProvider -> {
            newOfficialProvider.getMinecraftVersionExtractor().convention(namingChannel.getMinecraftVersionExtractor());
            newOfficialProvider.getApplySourceMappingsTaskBuilder().convention(context -> this.adaptApplySourceMappingsTask(context, namingChannel));
            newOfficialProvider.getApplyCompiledMappingsTaskBuilder().convention(context -> this.adaptApplyCompiledMappingsTask(context, namingChannel));
            newOfficialProvider.getUnapplyCompiledMappingsTaskBuilder().convention(context -> this.adaptUnapplyCompiledMappingsTask(context, namingChannel));
            newOfficialProvider.getUnapplyAccessTransformerMappingsTaskBuilder().convention(context -> this.adaptUnapplyAccessTransformerMappingsTask(context, namingChannel));
            newOfficialProvider.getHasAcceptedLicense().set(namingChannel.getHasAcceptedLicense());
            newOfficialProvider.getLicenseText().set(namingChannel.getLicenseText());
        });
        minecraftExtension.getMappings().getChannel().convention(minecraftExtension.getNamingChannelProviders().named("official"));
    }

    @NotNull
    private TaskProvider<? extends Runtime> adaptApplySourceMappingsTask(@NotNull final TaskBuildingContext context, @NotNull final NamingChannel namingChannel) {
        final TaskProvider<? extends Runtime> applySourceMappingsTask = namingChannel.getApplySourceMappingsTaskBuilder().get().build(context);

        final Optional<McpRuntimeDefinition> runtimeDefinition = context.getRuntimeDefinition()
                .filter(McpRuntimeDefinition.class::isInstance)
                .map(McpRuntimeDefinition.class::cast);

        if (!runtimeDefinition.isPresent()) {
            throw new IllegalStateException("The runtime definition is not present.");
        }

        final McpRuntimeDefinition mcpRuntimeDefinition = runtimeDefinition.get();
        final String mappingsFilePath = mcpRuntimeDefinition.getMcpConfig().getData("mappings");
        final File mappingsFile = new File(mcpRuntimeDefinition.getUnpackedMcpZipDirectory(), Objects.requireNonNull(mappingsFilePath));

        applySourceMappingsTask.configure(task -> {
            if (task instanceof ApplyMappingsToSourceJar) {
                final ApplyMappingsToSourceJar applyMappingsToSourceJar = (ApplyMappingsToSourceJar) task;
                applyMappingsToSourceJar.getSourceRenamer().set(
                        context.getClientMappings()
                                .flatMap(WithOutput::getOutput)
                                .flatMap(clientMappings ->
                                        context.getServerMappings()
                                                .flatMap(WithOutput::getOutput)
                                                .map(TransformerUtils.guard(serverMappings -> {
                                                    final IMappingFile clientMappingFile = IMappingFile.load(clientMappings.getAsFile()).reverse();
                                                    final IMappingFile serverMappingFile = IMappingFile.load(serverMappings.getAsFile()).reverse();
                                                    final IMappingFile mcpConfigMappings = IMappingFile.load(mappingsFile);
                                                    final IMappingFile reversedMcpConfigMappings = mcpConfigMappings.reverse();
                                                    return IMappingFileSourceRenamer.from(
                                                            reversedMcpConfigMappings.chain(clientMappingFile).reverse(),
                                                            reversedMcpConfigMappings.chain(serverMappingFile).reverse()
                                                    );
                                                }))
                                )
                );
            }
        });

        return applySourceMappingsTask;
    }

    @NotNull
    private TaskProvider<? extends WithOutput> adaptApplyCompiledMappingsTask(@NotNull final TaskBuildingContext context, @NotNull final NamingChannel namingChannel) {
        final TaskProvider<? extends WithOutput> applyCompiledMappingsTask = namingChannel.getApplyCompiledMappingsTaskBuilder().get().build(context);

        final TaskProvider<? extends Runtime> reverseMappingsTask = createReverseMappingWritingTaskFor(context, "reverseMappingsForApplyToCompiledFor%s");

        applyCompiledMappingsTask.configure(task -> {
            if (task instanceof ApplyOfficialMappingsToCompiledJar) {
                final ApplyOfficialMappingsToCompiledJar applyMappingsToCompiledJar = (ApplyOfficialMappingsToCompiledJar) task;
                applyMappingsToCompiledJar.getMappings().set(reverseMappingsTask.flatMap(WithOutput::getOutput));
                applyMappingsToCompiledJar.dependsOn(reverseMappingsTask);
            }
        });

        return applyCompiledMappingsTask;
    }

    @NotNull
    private TaskProvider<? extends WithOutput> adaptUnapplyCompiledMappingsTask(@NotNull final TaskBuildingContext context, @NotNull final NamingChannel namingChannel) {
        final TaskProvider<? extends WithOutput> unapplyCompiledMappingsTask = namingChannel.getUnapplyCompiledMappingsTaskBuilder().get().build(context);

        final TaskProvider<? extends Runtime> reverseMappingsTask = createReverseMappingWritingTaskFor(context, "reverseMappingsForUnapplyToCompiledFor%s");

        unapplyCompiledMappingsTask.configure(task -> {
            if (task instanceof UnapplyOfficialMappingsToCompiledJar) {
                final UnapplyOfficialMappingsToCompiledJar applyMappingsToCompiledJar = (UnapplyOfficialMappingsToCompiledJar) task;
                applyMappingsToCompiledJar.getMappings().set(reverseMappingsTask.flatMap(WithOutput::getOutput));
                applyMappingsToCompiledJar.dependsOn(reverseMappingsTask);
            }
        });

        return unapplyCompiledMappingsTask;
    }

    @NotNull
    private TaskProvider<? extends Runtime> adaptUnapplyAccessTransformerMappingsTask(TaskBuildingContext context, NamingChannel namingChannel) {
        final TaskProvider<? extends Runtime> applySourceMappingsTask = namingChannel.getUnapplyAccessTransformerMappingsTaskBuilder().get().build(context);

        final Optional<McpRuntimeDefinition> runtimeDefinition = context.getRuntimeDefinition()
                .filter(McpRuntimeDefinition.class::isInstance)
                .map(McpRuntimeDefinition.class::cast);

        if (!runtimeDefinition.isPresent()) {
            throw new IllegalStateException("The runtime definition is not present.");
        }

        final McpRuntimeDefinition mcpRuntimeDefinition = runtimeDefinition.get();
        final String mappingsFilePath = mcpRuntimeDefinition.getMcpConfig().getData("mappings");
        final File mappingsFile = new File(mcpRuntimeDefinition.getUnpackedMcpZipDirectory(), Objects.requireNonNull(mappingsFilePath));

        applySourceMappingsTask.configure(task -> {
            if (task instanceof UnapplyOfficialMappingsToAccessTransformer) {
                final UnapplyOfficialMappingsToAccessTransformer applyMappingsToSourceJar = (UnapplyOfficialMappingsToAccessTransformer) task;
                applyMappingsToSourceJar.getTypeRenamer().set(
                        context.getClientMappings()
                                .flatMap(WithOutput::getOutput)
                                .flatMap(clientMappings ->
                                        context.getServerMappings()
                                                .flatMap(WithOutput::getOutput)
                                                .map(TransformerUtils.guard(serverMappings -> {
                                                    final IMappingFile clientMappingFile = IMappingFile.load(clientMappings.getAsFile());
                                                    final IMappingFile serverMappingFile = IMappingFile.load(serverMappings.getAsFile());
                                                    final IMappingFile mcpConfigMappings = IMappingFile.load(mappingsFile);
                                                    final IMappingFile reversedMcpConfigMappings = mcpConfigMappings.reverse();
                                                    return IMappingFileTypeRenamer.from(
                                                            reversedMcpConfigMappings.chain(clientMappingFile.reverse()).reverse(),
                                                            reversedMcpConfigMappings.chain(serverMappingFile.reverse()).reverse()
                                                    );
                                                }))
                                )
                );
            }
        });

        return applySourceMappingsTask;
    }

    @NotNull
    private static TaskProvider<? extends Runtime> createReverseMappingWritingTaskFor(@NotNull TaskBuildingContext context, String format) {
        final Optional<McpRuntimeDefinition> runtimeDefinition = context.getRuntimeDefinition()
                .filter(McpRuntimeDefinition.class::isInstance)
                .map(McpRuntimeDefinition.class::cast);

        if (!runtimeDefinition.isPresent()) {
            throw new IllegalStateException("The runtime definition is not present.");
        }

        final McpRuntimeDefinition mcpRuntimeDefinition = runtimeDefinition.get();
        final String mappingsFilePath = mcpRuntimeDefinition.getMcpConfig().getData("mappings");
        final File mappingsFile = new File(mcpRuntimeDefinition.getUnpackedMcpZipDirectory(), Objects.requireNonNull(mappingsFilePath));

        final TaskProvider<? extends Runtime> reverseMappingsTask = context.getProject().getTasks().register(context.getTaskNameBuilder().apply(String.format(format, context.getEnvironmentName())), WriteIMappingsFile.class, task -> {
            task.getMappings().set(
                    context.getClientMappings()
                            .flatMap(WithOutput::getOutput)
                            .map(TransformerUtils.guard(
                                    clientMappingsFile -> {
                                        final IMappingFile clientMappingFile = IMappingFileUtils.load(clientMappingsFile.getAsFile());
                                        final IMappingFile mcpConfigMappings = IMappingFile.load(mappingsFile);
                                        final IMappingFile reversedMcpConfigMappings = mcpConfigMappings.reverse();
                                        final IMappingFile resultantFile = reversedMcpConfigMappings.chain(clientMappingFile);
                                        return new CacheableIMappingFile(resultantFile);
                                    }
                            ))
            );
        });

        context.addTask(reverseMappingsTask);

        return reverseMappingsTask;
    }

}
