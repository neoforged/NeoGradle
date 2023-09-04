package net.neoforged.gradle.userdev.runtime.definition;

import net.neoforged.gradle.common.dependency.ClientExtraJarDependencyManager;
import net.neoforged.gradle.common.dependency.MappingDebugChannelDependencyManager;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.userdev.configurations.UserDevConfigurationSpecV2;
import net.neoforged.gradle.dsl.userdev.runtime.definition.UserDevDefinition;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
import net.neoforged.gradle.userdev.runtime.tasks.ClasspathSerializer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a configured and registered runtime for forges userdev environment.
 */
public final class UserDevRuntimeDefinition extends CommonRuntimeDefinition<UserDevRuntimeSpecification> implements UserDevDefinition<UserDevRuntimeSpecification>, IDelegatingRuntimeDefinition<UserDevRuntimeSpecification> {
    private final NeoFormRuntimeDefinition mcpRuntimeDefinition;
    private final File unpackedUserDevJarDirectory;
    private final UserDevConfigurationSpecV2 userdevConfiguration;
    private final Configuration additionalUserDevDependencies;
    private final TaskProvider<ClasspathSerializer> minecraftClasspathSerializer;

    public UserDevRuntimeDefinition(@NotNull UserDevRuntimeSpecification specification, NeoFormRuntimeDefinition mcpRuntimeDefinition, File unpackedUserDevJarDirectory, UserDevConfigurationSpecV2 userdevConfiguration, Configuration additionalUserDevDependencies) {
        super(specification, mcpRuntimeDefinition.getTasks(), mcpRuntimeDefinition.getSourceJarTask(), mcpRuntimeDefinition.getRawJarTask(), mcpRuntimeDefinition.getGameArtifactProvidingTasks(), mcpRuntimeDefinition.getMinecraftDependenciesConfiguration(), mcpRuntimeDefinition::configureAssociatedTask);
        this.mcpRuntimeDefinition = mcpRuntimeDefinition;
        this.unpackedUserDevJarDirectory = unpackedUserDevJarDirectory;
        this.userdevConfiguration = userdevConfiguration;
        this.additionalUserDevDependencies = additionalUserDevDependencies;

        this.additionalUserDevDependencies.getDependencies().add(
                this.getSpecification().getProject().getDependencies().create(
                        ClientExtraJarDependencyManager.generateCoordinateFor(this.getSpecification().getMinecraftVersion())
                )
        );

        this.minecraftClasspathSerializer = specification.getProject().getTasks().register(
                CommonRuntimeUtils.buildStepName(getSpecification(), "writeMinecraftClasspath"),
                ClasspathSerializer.class,
                task -> {
                    task.getInputFiles().from(this.additionalUserDevDependencies);
                    task.getInputFiles().from(mcpRuntimeDefinition.getMinecraftDependenciesConfiguration());
                }
        );
        configureAssociatedTask(this.minecraftClasspathSerializer);
    }

    @Override
    public NeoFormRuntimeDefinition getMcpRuntimeDefinition() {
        return mcpRuntimeDefinition;
    }

    @Override
    public File getUnpackedUserDevJarDirectory() {
        return unpackedUserDevJarDirectory;
    }

    @Override
    public UserDevConfigurationSpecV2 getUserdevConfiguration() {
        return userdevConfiguration;
    }

    @Override
    public Configuration getAdditionalUserDevDependencies() {
        return additionalUserDevDependencies;
    }

    @Override
    public void setReplacedDependency(@NotNull Dependency dependency) {
        super.setReplacedDependency(dependency);
        mcpRuntimeDefinition.setReplacedDependency(dependency);
    }


    @Override
    public void onRepoWritten(@NotNull final TaskProvider<? extends WithOutput> finalRepoWritingTask) {
        mcpRuntimeDefinition.onRepoWritten(finalRepoWritingTask);
        this.minecraftClasspathSerializer.configure(task -> {
            task.getInputFiles().from(finalRepoWritingTask);
        });
    }

    @Override
    public @NotNull TaskProvider<DownloadAssets> getAssetsTaskProvider() {
        return mcpRuntimeDefinition.getAssetsTaskProvider();
    }

    @Override
    public @NotNull TaskProvider<ExtractNatives> getNativesTaskProvider() {
        return mcpRuntimeDefinition.getNativesTaskProvider();
    }

    @Override
    public @NotNull TaskProvider<? extends WithOutput> getDebuggingMappingsTaskProvider() {
        return mcpRuntimeDefinition.getDebuggingMappingsTaskProvider();
    }

    @Override
    public @NotNull TaskProvider<? extends WithOutput> getRuntimeToSourceMappingsTaskProvider() {
        return mcpRuntimeDefinition.getRuntimeToSourceMappingsTaskProvider();
    }

    @Override
    public @NotNull Map<String, String> getMappingVersionData() {
        return mcpRuntimeDefinition.getMappingVersionData();
    }

    @Override
    public void configureRun(RunImpl run) {
        super.configureRun(run);
        run.getClasspath().from(getDebuggingMappingsTaskProvider());
        run.dependsOn(this.minecraftClasspathSerializer);
    }

    @Override
    public @NotNull TaskProvider<? extends WithOutput> getRuntimeMappedRawJarTaskProvider() {
        return mcpRuntimeDefinition.getRuntimeMappedRawJarTaskProvider();
    }

    @NotNull
    @Override
    public TaskProvider<? extends WithOutput> getListLibrariesTaskProvider() {
        return mcpRuntimeDefinition.getListLibrariesTaskProvider();
    }

    @NotNull
    public TaskProvider<ClasspathSerializer> getMinecraftClasspathSerializer() {
        return minecraftClasspathSerializer;
    }

    @Override
    protected Map<String, String> buildRunInterpolationData() {
        final Map<String, String> interpolationData = mcpRuntimeDefinition.buildRunInterpolationData();

        if (userdevConfiguration.getModules() != null && !userdevConfiguration.getModules().isEmpty()) {
            final String name = String.format("moduleResolverForgeUserDev%s", getSpecification().getVersionedName());
            final Configuration modulesCfg;
            if (getSpecification().getProject().getConfigurations().getNames().contains(name)) {
                modulesCfg = getSpecification().getProject().getConfigurations().getByName(name);
            }
            else {
                modulesCfg = getSpecification().getProject().getConfigurations().create(name);
                modulesCfg.setCanBeResolved(true);
                userdevConfiguration.getModules().forEach(m -> modulesCfg.getDependencies().add(getSpecification().getProject().getDependencies().create(m)));
            }

            interpolationData.put("modules", modulesCfg.resolve().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
        }

        interpolationData.put("minecraft_classpath_file", this.minecraftClasspathSerializer.get().getOutput().get().getAsFile().getAbsolutePath());

        return interpolationData;
    }

    @Override
    public Definition<?> getDelegate() {
        return mcpRuntimeDefinition;
    }

    @Override
    public void onBake(NamingChannel namingChannel, File runtimeDirectory) {
        this.additionalUserDevDependencies.getDependencies().add(
                this.getSpecification().getProject().getDependencies().create(
                        MappingDebugChannelDependencyManager.generateCoordinateFor(
                                namingChannel,
                                mcpRuntimeDefinition.getMappingVersionData(),
                                this
                        )
                )
        );
    }
}
