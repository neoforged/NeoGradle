package net.minecraftforge.gradle.userdev.runtime.definition;

import net.minecraftforge.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.tasks.ClientExtraJar;
import net.minecraftforge.gradle.common.runtime.tasks.DownloadAssets;
import net.minecraftforge.gradle.common.runtime.tasks.ExtractNatives;
import net.minecraftforge.gradle.dsl.common.runtime.definition.Definition;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.dsl.userdev.configurations.UserDevConfigurationSpecV2;
import net.minecraftforge.gradle.dsl.userdev.runtime.definition.UserDevDefinition;
import net.minecraftforge.gradle.mcp.runtime.definition.McpRuntimeDefinition;
import net.minecraftforge.gradle.runs.run.RunImpl;
import net.minecraftforge.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
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
    private final McpRuntimeDefinition mcpRuntimeDefinition;
    private final File unpackedUserDevJarDirectory;
    private final UserDevConfigurationSpecV2 userdevConfiguration;
    private final Configuration additionalUserDevDependencies;

    public UserDevRuntimeDefinition(@NotNull UserDevRuntimeSpecification specification, McpRuntimeDefinition mcpRuntimeDefinition, File unpackedUserDevJarDirectory, UserDevConfigurationSpecV2 userdevConfiguration, Configuration additionalUserDevDependencies) {
        super(specification, mcpRuntimeDefinition.getTasks(), mcpRuntimeDefinition.getSourceJarTask(), mcpRuntimeDefinition.getRawJarTask(), mcpRuntimeDefinition.getGameArtifactProvidingTasks(), mcpRuntimeDefinition.getMinecraftDependenciesConfiguration(), mcpRuntimeDefinition::configureAssociatedTask);
        this.mcpRuntimeDefinition = mcpRuntimeDefinition;
        this.unpackedUserDevJarDirectory = unpackedUserDevJarDirectory;
        this.userdevConfiguration = userdevConfiguration;
        this.additionalUserDevDependencies = additionalUserDevDependencies;
    }

    @Override
    public McpRuntimeDefinition getMcpRuntimeDefinition() {
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

    @NotNull
    public TaskProvider<ClientExtraJar> getClientExtraJarProvider() {
        return mcpRuntimeDefinition.getClientExtraJarProvider();
    }

    @Override
    public @NotNull TaskProvider<DownloadAssets> getAssetsTaskProvider() {
        return mcpRuntimeDefinition.getAssetsTaskProvider();
    }

    @Override
    public @NotNull TaskProvider<ExtractNatives> getNativesTaskProvider() {
        return mcpRuntimeDefinition.getNativesTaskProvider();
    }

    public @NotNull TaskProvider<? extends WithOutput> getDebuggingMappingsTaskProvider() {
        return mcpRuntimeDefinition.getDebuggingMappingsTaskProvider();
    }

    @Override
    public @NotNull Map<String, String> getMappingVersionData() {
        return mcpRuntimeDefinition.getMappingVersionData();
    }

    @Override
    public void configureRun(RunImpl run) {
        super.configureRun(run);
        run.getClasspath().from(getDebuggingMappingsTaskProvider());
    }

    @Override
    protected Map<String, String> buildRunInterpolationData() {
        final Map<String, String> interpolationData = mcpRuntimeDefinition.buildRunInterpolationData();

        if (userdevConfiguration.getModules() != null && !userdevConfiguration.getModules().isEmpty()) {
            final String name = String.format("moduleResolverForgeUserDev%s", getSpecification().getName());
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

        return interpolationData;
    }

    @Override
    public Definition<?> getDelegate() {
        return mcpRuntimeDefinition;
    }
}
