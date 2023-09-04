package net.neoforged.gradle.neoform.naming;

import net.minecraftforge.srgutils.IMappingFile;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.naming.renamer.IMappingFileSourceRenamer;
import net.neoforged.gradle.common.runtime.naming.renamer.IMappingFileTypeRenamer;
import net.neoforged.gradle.common.runtime.naming.tasks.ApplyMappingsToSourceJar;
import net.neoforged.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToCompiledJar;
import net.neoforged.gradle.common.runtime.naming.tasks.UnapplyOfficialMappingsToAccessTransformer;
import net.neoforged.gradle.common.runtime.naming.tasks.UnapplyOfficialMappingsToCompiledJar;
import net.neoforged.gradle.common.tasks.WriteIMappingsFile;
import net.neoforged.gradle.common.util.CacheableIMappingFile;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runtime.naming.GenerationTaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.util.IMappingFileUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

public final class NeoFormOfficialNamingChannelConfigurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoFormOfficialNamingChannelConfigurator.class);
    private static final NeoFormOfficialNamingChannelConfigurator INSTANCE = new NeoFormOfficialNamingChannelConfigurator();

    private NeoFormOfficialNamingChannelConfigurator() {
    }

    public static NeoFormOfficialNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    public void configure(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);

        final NamingChannel namingChannel = minecraftExtension.getNamingChannels().getByName("official");
        minecraftExtension.getNamingChannels().remove(namingChannel);

        minecraftExtension.getNamingChannels().register("official", newOfficialProvider -> {
            newOfficialProvider.getMinecraftVersionExtractor().convention(namingChannel.getMinecraftVersionExtractor());
            newOfficialProvider.getApplySourceMappingsTaskBuilder().convention(context -> this.adaptApplySourceMappingsTask(context, namingChannel));
            newOfficialProvider.getApplyCompiledMappingsTaskBuilder().convention(context -> this.adaptApplyCompiledMappingsTask(context, namingChannel));
            newOfficialProvider.getUnapplyCompiledMappingsTaskBuilder().convention(context -> this.adaptUnapplyCompiledMappingsTask(context, namingChannel));
            newOfficialProvider.getUnapplyAccessTransformerMappingsTaskBuilder().convention(context -> this.adaptUnapplyAccessTransformerMappingsTask(context, namingChannel));
            newOfficialProvider.getRuntimeToSourceMappingsTaskBuilder().convention(this::buildRuntimeToSourceMappingsTask);
            newOfficialProvider.getHasAcceptedLicense().convention(namingChannel.getHasAcceptedLicense());
            newOfficialProvider.getLicenseText().convention(namingChannel.getLicenseText());
            newOfficialProvider.getDependencyNotationVersionManager().convention(namingChannel.getDependencyNotationVersionManager());
        });
        minecraftExtension.getMappings().getChannel().convention(minecraftExtension.getNamingChannels().named("official"));
    }

    @NotNull
    private TaskProvider<? extends Runtime> adaptApplySourceMappingsTask(@NotNull final TaskBuildingContext context, @NotNull final NamingChannel namingChannel) {
        final TaskProvider<? extends Runtime> applySourceMappingsTask = namingChannel.getApplySourceMappingsTaskBuilder().get().build(context);

        Optional<NeoFormRuntimeDefinition> runtimeDefinition = context.getRuntimeDefinition()
                .filter(NeoFormRuntimeDefinition.class::isInstance)
                .map(NeoFormRuntimeDefinition.class::cast);

        if (!runtimeDefinition.isPresent()) {
            //Resolve delegation
            runtimeDefinition = context.getRuntimeDefinition()
                    .filter(IDelegatingRuntimeDefinition.class::isInstance)
                    .map(IDelegatingRuntimeDefinition.class::cast)
                    .map(IDelegatingRuntimeDefinition::getDelegate)
                    .filter(NeoFormRuntimeDefinition.class::isInstance)
                    .map(NeoFormRuntimeDefinition.class::cast);
        }

        if (!runtimeDefinition.isPresent()) {
            throw new IllegalStateException("The runtime definition is not present.");
        }

        final NeoFormRuntimeDefinition mcpRuntimeDefinition = runtimeDefinition.get();
        final String mappingsFilePath = mcpRuntimeDefinition.getNeoFormConfig().getData("mappings");
        final File mappingsFile = new File(mcpRuntimeDefinition.getUnpackedNeoFormZipDirectory(), Objects.requireNonNull(mappingsFilePath));

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
    private TaskProvider<? extends Runtime> adaptApplyCompiledMappingsTask(@NotNull final TaskBuildingContext context, @NotNull final NamingChannel namingChannel) {
        final TaskProvider<? extends Runtime> applyCompiledMappingsTask = namingChannel.getApplyCompiledMappingsTaskBuilder().get().build(context);

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
    private TaskProvider<? extends Runtime> adaptUnapplyCompiledMappingsTask(@NotNull final TaskBuildingContext context, @NotNull final NamingChannel namingChannel) {
        final TaskProvider<? extends Runtime> unapplyCompiledMappingsTask = namingChannel.getUnapplyCompiledMappingsTaskBuilder().get().build(context);

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

        Optional<NeoFormRuntimeDefinition> runtimeDefinition = context.getRuntimeDefinition()
                .filter(NeoFormRuntimeDefinition.class::isInstance)
                .map(NeoFormRuntimeDefinition.class::cast);

        if (!runtimeDefinition.isPresent()) {
            //Resolve delegation
            runtimeDefinition = context.getRuntimeDefinition()
                    .filter(IDelegatingRuntimeDefinition.class::isInstance)
                    .map(IDelegatingRuntimeDefinition.class::cast)
                    .map(IDelegatingRuntimeDefinition::getDelegate)
                    .filter(NeoFormRuntimeDefinition.class::isInstance)
                    .map(NeoFormRuntimeDefinition.class::cast);
        }

        if (!runtimeDefinition.isPresent()) {
            throw new IllegalStateException("The runtime definition is not present.");
        }

        final NeoFormRuntimeDefinition mcpRuntimeDefinition = runtimeDefinition.get();
        final String mappingsFilePath = mcpRuntimeDefinition.getNeoFormConfig().getData("mappings");
        final File mappingsFile = new File(mcpRuntimeDefinition.getUnpackedNeoFormZipDirectory(), Objects.requireNonNull(mappingsFilePath));

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
        Optional<NeoFormRuntimeDefinition> runtimeDefinition = context.getRuntimeDefinition()
                .filter(NeoFormRuntimeDefinition.class::isInstance)
                .map(NeoFormRuntimeDefinition.class::cast);

        if (!runtimeDefinition.isPresent()) {
            //Resolve delegation
            runtimeDefinition = context.getRuntimeDefinition()
                    .filter(IDelegatingRuntimeDefinition.class::isInstance)
                    .map(IDelegatingRuntimeDefinition.class::cast)
                    .map(IDelegatingRuntimeDefinition::getDelegate)
                    .filter(NeoFormRuntimeDefinition.class::isInstance)
                    .map(NeoFormRuntimeDefinition.class::cast);
        }

        if (!runtimeDefinition.isPresent()) {
            throw new IllegalStateException("The runtime definition is not present.");
        }

        final NeoFormRuntimeDefinition mcpRuntimeDefinition = runtimeDefinition.get();
        final String mappingsFilePath = mcpRuntimeDefinition.getNeoFormConfig().getData("mappings");
        final File mappingsFile = new File(mcpRuntimeDefinition.getUnpackedNeoFormZipDirectory(), Objects.requireNonNull(mappingsFilePath));

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

    private @NotNull TaskProvider<? extends Runtime> buildRuntimeToSourceMappingsTask(@NotNull final GenerationTaskBuildingContext context) {
        Optional<NeoFormRuntimeDefinition> runtimeDefinition = context.getRuntimeDefinition()
                .filter(NeoFormRuntimeDefinition.class::isInstance)
                .map(NeoFormRuntimeDefinition.class::cast);

        if (!runtimeDefinition.isPresent()) {
            //Resolve delegation
            runtimeDefinition = context.getRuntimeDefinition()
                    .filter(IDelegatingRuntimeDefinition.class::isInstance)
                    .map(IDelegatingRuntimeDefinition.class::cast)
                    .map(IDelegatingRuntimeDefinition::getDelegate)
                    .filter(NeoFormRuntimeDefinition.class::isInstance)
                    .map(NeoFormRuntimeDefinition.class::cast);
        }

        if (!runtimeDefinition.isPresent()) {
            throw new IllegalStateException("The runtime definition is not present.");
        }

        final NeoFormRuntimeDefinition mcpRuntimeDefinition = runtimeDefinition.get();
        final String mappingsFilePath = mcpRuntimeDefinition.getNeoFormConfig().getData("mappings");
        final File mappingsFile = new File(mcpRuntimeDefinition.getUnpackedNeoFormZipDirectory(), Objects.requireNonNull(mappingsFilePath));

        final String writeRuntimeToSourceMappingsTaskName = context.getTaskNameBuilder().apply("writeRuntimeToSourceMappings");

        return context.getProject().getTasks().register(writeRuntimeToSourceMappingsTaskName, WriteIMappingsFile.class, task -> {
            task.setGroup("mappings/official");
            task.setDescription("Writes the mapping file from runtime to source mappings");
            task.getFormat().set(IMappingFile.Format.TSRG2);
            task.getMappings().set(
                    context.getClientMappings()
                            .flatMap(WithOutput::getOutput)
                            .map(TransformerUtils.guard(
                                    clientMappingsFile -> {
                                        final IMappingFile clientMappingFile = IMappingFileUtils.load(clientMappingsFile.getAsFile());
                                        final IMappingFile mcpConfigMappings = IMappingFile.load(mappingsFile);
                                        final IMappingFile reversedMcpConfigMappings = mcpConfigMappings.reverse();
                                        final IMappingFile resultantFile = reversedMcpConfigMappings.chain(clientMappingFile.reverse());
                                        return new CacheableIMappingFile(resultantFile);
                                    }
                            ))
            );
            task.dependsOn(context.getClientMappings());
        });
    }
}
