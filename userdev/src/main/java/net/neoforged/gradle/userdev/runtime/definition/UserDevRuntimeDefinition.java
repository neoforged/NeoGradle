package net.neoforged.gradle.userdev.runtime.definition;

import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.dsl.userdev.runtime.definition.UserDevDefinition;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
import net.neoforged.gradle.userdev.runtime.tasks.ClasspathSerializer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileTree;
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
    private final FileTree unpackedUserDevJarDirectory;
    private final UserdevProfile userdevConfiguration;
    private final Configuration additionalUserDevDependencies;

    private TaskProvider<? extends WithOutput> userdevClasspathElementProducer;

    public UserDevRuntimeDefinition(@NotNull UserDevRuntimeSpecification specification, NeoFormRuntimeDefinition neoformRuntimeDefinition, FileTree unpackedUserDevJarDirectory, UserdevProfile userdevConfiguration, Configuration additionalUserDevDependencies) {
        super(specification, neoformRuntimeDefinition.getTasks(), neoformRuntimeDefinition.getSourceJarTask(), neoformRuntimeDefinition.getRawJarTask(), neoformRuntimeDefinition.getGameArtifactProvidingTasks(), neoformRuntimeDefinition.getMinecraftDependenciesConfiguration(), neoformRuntimeDefinition::configureAssociatedTask, neoformRuntimeDefinition.getVersionJson());
        this.neoformRuntimeDefinition = neoformRuntimeDefinition;
        this.unpackedUserDevJarDirectory = unpackedUserDevJarDirectory;
        this.userdevConfiguration = userdevConfiguration;
        this.additionalUserDevDependencies = additionalUserDevDependencies;

        //Create the client-extra jar dependency.
        final Dependency clientExtraJar = this.getSpecification().getProject().getDependencies().create(
                ExtraJarDependencyManager.generateClientCoordinateFor(this.getSpecification().getMinecraftVersion())
        );

        //Add it as a user dev dependency, this will trigger replacement, which will need to be addressed down-below.
        this.additionalUserDevDependencies.getDependencies().add(
                clientExtraJar
        );

        this.getAllDependencies().from(neoformRuntimeDefinition.getAllDependencies());
        this.getAllDependencies().from(getAdditionalUserDevDependencies());
        this.getAllDependencies().from(getUserdevConfiguration());
    }

    @Override
    public NeoFormRuntimeDefinition getNeoFormRuntimeDefinition() {
        return neoformRuntimeDefinition;
    }

    @Override
    public FileTree getUnpackedUserDevJarDirectory() {
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

    @NotNull
    @Override
    public TaskProvider<? extends WithOutput> getListLibrariesTaskProvider() {
        return neoformRuntimeDefinition.getListLibrariesTaskProvider();
    }

    @Override
    protected Map<String, String> buildRunInterpolationData(RunImpl run) {
        final Map<String, String> interpolationData = neoformRuntimeDefinition.buildRunInterpolationData(run);

        if (userdevConfiguration.getModules() != null && !userdevConfiguration.getModules().get().isEmpty()) {
            final String name = String.format("moduleResolverForgeUserDev%s", getSpecification().getVersionedName());
            final Configuration modulesCfg;
            if (getSpecification().getProject().getConfigurations().getNames().contains(name)) {
                modulesCfg = getSpecification().getProject().getConfigurations().getByName(name);
            } else {
                modulesCfg = getSpecification().getProject().getConfigurations().create(name);
                modulesCfg.setCanBeResolved(true);
                userdevConfiguration.getModules().get().forEach(m -> modulesCfg.getDependencies().add(getSpecification().getProject().getDependencies().create(m)));
            }

            interpolationData.put("modules", modulesCfg.resolve().stream().map(File::getAbsolutePath).collect(Collectors.joining(File.pathSeparator)));
        }

        final TaskProvider<ClasspathSerializer> minecraftClasspathSerializer = getSpecification().getProject().getTasks().register(
                RunsUtil.createTaskName("writeMinecraftClasspath", run),
                ClasspathSerializer.class,
                task -> {
                    this.additionalUserDevDependencies.getExtendsFrom().forEach(task.getInputFiles()::from);
                    task.getInputFiles().from(this.additionalUserDevDependencies);
                    task.getInputFiles().from(neoformRuntimeDefinition.getMinecraftDependenciesConfiguration());
                    task.getInputFiles().from(this.userdevClasspathElementProducer.flatMap(WithOutput::getOutput));

                    Configuration userDependencies = run.getDependencies().get().getRuntimeConfiguration();
                    task.getInputFiles().from(userDependencies);
                }
        );
        configureAssociatedTask(minecraftClasspathSerializer);

        interpolationData.put("minecraft_classpath_file", minecraftClasspathSerializer.get().getOutput().get().getAsFile().getAbsolutePath());

        run.dependsOn(minecraftClasspathSerializer);

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

    public TaskProvider<? extends WithOutput> getUserdevClasspathElementProducer() {
        return userdevClasspathElementProducer;
    }

    public void setUserdevClasspathElementProducer(TaskProvider<? extends WithOutput> userdevClasspathElementProducer) {
        this.userdevClasspathElementProducer = userdevClasspathElementProducer;
    }
}
