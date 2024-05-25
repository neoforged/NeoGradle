package net.neoforged.gradle.neoform.naming;

import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.naming.renamer.IMappingFileSourceRenamer;
import net.neoforged.gradle.common.runtime.naming.renamer.ISourceRenamer;
import net.neoforged.gradle.common.runtime.naming.tasks.ApplyMappingsToSourceJar;
import net.neoforged.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToCompiledJar;
import net.neoforged.gradle.common.tasks.WriteIMappingsFile;
import net.neoforged.gradle.common.util.CacheableIMappingFile;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.util.IMappingFileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;
import java.util.function.BiFunction;

public final class NeoFormOfficialNamingChannelConfigurator {
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
            newOfficialProvider.getApplySourceMappingsTaskBuilder().convention(context -> this.adaptApplySourceMappingsTask(context, namingChannel));
            newOfficialProvider.getApplyCompiledMappingsTaskBuilder().convention(context -> this.adaptApplyCompiledMappingsTask(context, namingChannel));
            newOfficialProvider.getHasAcceptedLicense().convention(namingChannel.getHasAcceptedLicense());
            newOfficialProvider.getLicenseText().convention(namingChannel.getLicenseText());
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

        final NeoFormRuntimeDefinition neoformRuntimeDefinition = runtimeDefinition.get();
        final Provider<File> neoformMappings = context.getProject().getProviders().of(MappingsFileValueSource.class, spec -> {
            spec.getParameters().getNeoFormArchive().from(context.getProject().fileTree(neoformRuntimeDefinition.getSpecification().getNeoFormArchive()));
            spec.getParameters().getMappingsFilePath().set(neoformRuntimeDefinition.getNeoFormConfig().getData("mappings"));
        });

        applySourceMappingsTask.configure(task -> {
            if (task instanceof ApplyMappingsToSourceJar) {
                final ApplyMappingsToSourceJar applyMappingsToSourceJar = (ApplyMappingsToSourceJar) task;
                applyMappingsToSourceJar.getSourceRenamer().set(
                        context.getClientMappings()
                                .flatMap(WithOutput::getOutput)
                                .flatMap(clientMappings ->
                                        context.getServerMappings()
                                                .flatMap(WithOutput::getOutput)
                                                .zip(neoformMappings, (BiFunction<RegularFile, File, ISourceRenamer>) (serverMappings, neoformMappingsFile) -> {
                                                    final IMappingFile clientMappingFile = IMappingFileUtils.load(clientMappings.getAsFile()).reverse();
                                                    final IMappingFile serverMappingFile = IMappingFileUtils.load(serverMappings.getAsFile()).reverse();
                                                    final IMappingFile mcpConfigMappings = IMappingFileUtils.load(neoformMappingsFile);
                                                    final IMappingFile reversedMcpConfigMappings = mcpConfigMappings.reverse();
                                                    return IMappingFileSourceRenamer.from(
                                                            reversedMcpConfigMappings.chain(clientMappingFile).reverse(),
                                                            reversedMcpConfigMappings.chain(serverMappingFile).reverse()
                                                    );
                                                })
                                )
                );
            }
        });

        return applySourceMappingsTask;
    }
    
    @NotNull
    private TaskProvider<? extends Runtime> adaptApplyCompiledMappingsTask(@NotNull final TaskBuildingContext context, @NotNull final NamingChannel namingChannel) {
        final TaskProvider<? extends Runtime> applyCompiledMappingsTask = namingChannel.getApplyCompiledMappingsTaskBuilder().get().build(context);
        
        final TaskProvider<? extends Runtime> combineMappingsTask = createCombinedMappingsFor(context);
        
        applyCompiledMappingsTask.configure(task -> {
            if (task instanceof ApplyOfficialMappingsToCompiledJar) {
                final ApplyOfficialMappingsToCompiledJar applyMappingsToCompiledJar = (ApplyOfficialMappingsToCompiledJar) task;
                applyMappingsToCompiledJar.getMappings().set(combineMappingsTask.flatMap(WithOutput::getOutput));
                applyMappingsToCompiledJar.getShouldReverseMappings().set(false);
                applyMappingsToCompiledJar.dependsOn(combineMappingsTask);
            }
        });
        
        return applyCompiledMappingsTask;
    }
    
    @NotNull
    private static TaskProvider<? extends Runtime> createCombinedMappingsFor(@NotNull TaskBuildingContext context) {
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
        
        final NeoFormRuntimeDefinition neoformRuntimeDefinition = runtimeDefinition.get();
        final Provider<File> neoformMappings = context.getProject().getProviders().of(MappingsFileValueSource.class, spec -> {
            spec.getParameters().getNeoFormArchive().from(context.getProject().fileTree(neoformRuntimeDefinition.getSpecification().getNeoFormArchive()));
            spec.getParameters().getMappingsFilePath().set(neoformRuntimeDefinition.getNeoFormConfig().getData("mappings"));
        });

        final TaskProvider<? extends Runtime> reverseMappingsTask = context.getProject().getTasks().register(context.getTaskNameBuilder().apply(String.format("combineMappingsFor%s", StringUtils.capitalize(context.getEnvironmentName()))), WriteIMappingsFile.class, task -> {
            task.getMappings().set(
                    context.getClientMappings()
                            .flatMap(WithOutput::getOutput)
                            .zip(neoformMappings, (clientMappingsFile, neoformMappingsFile) -> {
                                final IMappingFile neoformConfigMappings = IMappingFileUtils.load(neoformMappingsFile); // OBF -> OBF + PARAM
                                final IMappingFile clientMappingFile = IMappingFileUtils.load(clientMappingsFile.getAsFile()).reverse(); // MOJ -> OBF, reversing so that it becomes OBF -> MOJ
                                return new CacheableIMappingFile(neoformConfigMappings.rename(makeRenamer(clientMappingFile, true, true, true, false)));//OBF -> OBF + PARAM -> MOJ + PARAM
                            })
            );
        });
        
        context.addTask(reverseMappingsTask);
        
        return reverseMappingsTask;
    }
    
    @SuppressWarnings("SameParameterValue")
    private static IRenamer makeRenamer(IMappingFile link, boolean classes, boolean fields, boolean methods, boolean params) {
        return new IRenamer() {
            public String rename(IMappingFile.IPackage value) {
                return link.remapPackage(value.getOriginal());
            }
            
            public String rename(IMappingFile.IClass value) {
                return classes ? link.remapClass(value.getOriginal()) : value.getMapped();
            }
            
            public String rename(IMappingFile.IField value) {
                IMappingFile.IClass cls = link.getClass(value.getParent().getOriginal());
                return cls == null || !fields ? value.getMapped() : cls.remapField(value.getOriginal());
            }
            
            public String rename(IMappingFile.IMethod value) {
                IMappingFile.IClass cls = link.getClass(value.getParent().getOriginal());
                return cls == null || !methods ? value.getMapped() : cls.remapMethod(value.getOriginal(), value.getDescriptor());
            }
            
            public String rename(IMappingFile.IParameter value) {
                IMappingFile.IMethod mtd = value.getParent();
                IMappingFile.IClass cls = link.getClass(mtd.getParent().getOriginal());
                mtd = cls == null ? null : cls.getMethod(mtd.getOriginal(), mtd.getDescriptor());
                return mtd == null || !params ? value.getMapped() : mtd.remapParameter(value.getIndex(), value.getMapped());
            }
        };
    }

    public interface MappingsFileValueSourceParameters extends ValueSourceParameters {

        Property<String> getMappingsFilePath();

        ConfigurableFileCollection getNeoFormArchive();
    }

    public abstract static class MappingsFileValueSource implements ValueSource<File, MappingsFileValueSourceParameters> {

        @Nullable
        @Override
        public File obtain() {
            return getParameters().getNeoFormArchive().filter(f -> f.getAbsoluteFile().getPath().endsWith(getParameters().getMappingsFilePath().get())).getSingleFile();
        }
    }
}
