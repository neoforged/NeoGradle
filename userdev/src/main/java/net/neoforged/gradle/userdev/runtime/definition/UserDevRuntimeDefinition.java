package net.neoforged.gradle.userdev.runtime.definition;

import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.dsl.userdev.runtime.definition.UserDevDefinition;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
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
    private final NeoFormRuntimeDefinition neoformRuntimeDefinition;
    private final File unpackedUserDevJarDirectory;
    private final UserdevProfile userdevConfiguration;
    private final Configuration additionalUserDevDependencies;
    private final TaskProvider<ClasspathSerializer> minecraftClasspathSerializer;

    public UserDevRuntimeDefinition(@NotNull UserDevRuntimeSpecification specification, NeoFormRuntimeDefinition neoformRuntimeDefinition, File unpackedUserDevJarDirectory, UserdevProfile userdevConfiguration, Configuration additionalUserDevDependencies) {
        super(specification, neoformRuntimeDefinition.getTasks(), neoformRuntimeDefinition.getSourceJarTask(), neoformRuntimeDefinition.getRawJarTask(), neoformRuntimeDefinition.getGameArtifactProvidingTasks(), neoformRuntimeDefinition.getMinecraftDependenciesConfiguration(), neoformRuntimeDefinition::configureAssociatedTask, neoformRuntimeDefinition.getVersionJson());
        this.neoformRuntimeDefinition = neoformRuntimeDefinition;
        this.unpackedUserDevJarDirectory = unpackedUserDevJarDirectory;
        this.userdevConfiguration = userdevConfiguration;
        this.additionalUserDevDependencies = additionalUserDevDependencies;

        this.additionalUserDevDependencies.getDependencies().add(
                this.getSpecification().getProject().getDependencies().create(
                        ExtraJarDependencyManager.generateClientCoordinateFor(this.getSpecification().getMinecraftVersion())
                )
        );

        this.minecraftClasspathSerializer = specification.getProject().getTasks().register(
                CommonRuntimeUtils.buildStepName(getSpecification(), "writeMinecraftClasspath"),
                ClasspathSerializer.class,
                task -> {
                    this.additionalUserDevDependencies.getExtendsFrom().forEach(task.getInputFiles()::from);
                    task.getInputFiles().from(this.additionalUserDevDependencies);
                    task.getInputFiles().from(neoformRuntimeDefinition.getMinecraftDependenciesConfiguration());
                }
        );
        configureAssociatedTask(this.minecraftClasspathSerializer);
    }

    @Override
    public NeoFormRuntimeDefinition getNeoFormRuntimeDefinition() {
        return neoformRuntimeDefinition;
    }

    @Override
    public File getUnpackedUserDevJarDirectory() {
        return unpackedUserDevJarDirectory;
    }

    @Override
    public UserdevProfile getUserdevConfiguration() {
        return userdevConfiguration;
    }

    @Override
    public Configuration getAdditionalUserDevDependencies() {
        return additionalUserDevDependencies;
    }

    @Override
    public void setReplacedDependency(@NotNull Dependency dependency) {
        super.setReplacedDependency(dependency);
        neoformRuntimeDefinition.setReplacedDependency(dependency);
    }


    @Override
    public void onRepoWritten(@NotNull final TaskProvider<? extends WithOutput> finalRepoWritingTask) {
        neoformRuntimeDefinition.onRepoWritten(finalRepoWritingTask);
        this.minecraftClasspathSerializer.configure(task -> {
            task.getInputFiles().from(finalRepoWritingTask);
        });
    }

    @Override
    public @NotNull TaskProvider<DownloadAssets> getAssets() {
        return neoformRuntimeDefinition.getAssets();
    }

    @Override
    public @NotNull TaskProvider<ExtractNatives> getNatives() {
        return neoformRuntimeDefinition.getNatives();
    }

    @Override
    public @NotNull Map<String, String> getMappingVersionData() {
        return neoformRuntimeDefinition.getMappingVersionData();
    }

    @Override
    public void configureRun(RunImpl run) {
        super.configureRun(run);
        run.dependsOn(this.minecraftClasspathSerializer);
    }

    @NotNull
    @Override
    public TaskProvider<? extends WithOutput> getListLibrariesTaskProvider() {
        return neoformRuntimeDefinition.getListLibrariesTaskProvider();
    }

    @NotNull
    public TaskProvider<ClasspathSerializer> getMinecraftClasspathSerializer() {
        return minecraftClasspathSerializer;
    }

    @Override
    protected Map<String, String> buildRunInterpolationData() {
        final Map<String, String> interpolationData = neoformRuntimeDefinition.buildRunInterpolationData();

        if (userdevConfiguration.getModules() != null && !userdevConfiguration.getModules().get().isEmpty()) {
            final String name = String.format("moduleResolverForgeUserDev%s", getSpecification().getVersionedName());
            final Configuration modulesCfg;
            if (getSpecification().getProject().getConfigurations().getNames().contains(name)) {
                modulesCfg = getSpecification().getProject().getConfigurations().getByName(name);
            }
            else {
                modulesCfg = getSpecification().getProject().getConfigurations().create(name);
                modulesCfg.setCanBeResolved(true);
                userdevConfiguration.getModules().get().forEach(m -> modulesCfg.getDependencies().add(getSpecification().getProject().getDependencies().create(m)));
            }

            interpolationData.put("modules", modulesCfg.resolve().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
        }

        interpolationData.put("minecraft_classpath_file", this.minecraftClasspathSerializer.get().getOutput().get().getAsFile().getAbsolutePath());

        return interpolationData;
    }

    @Override
    public Definition<?> getDelegate() {
        return neoformRuntimeDefinition;
    }
    
    @Override
    public @NotNull VersionJson getVersionJson() {
        return getNeoFormRuntimeDefinition().getVersionJson();
    }
}
