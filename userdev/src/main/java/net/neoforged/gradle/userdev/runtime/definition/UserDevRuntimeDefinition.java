package net.neoforged.gradle.userdev.runtime.definition;

import net.neoforged.gradle.common.dependency.ExtraJarDependencyManager;
import net.neoforged.gradle.common.extensions.problems.IProblemReporter;
import net.neoforged.gradle.common.runs.run.RunImpl;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.tasks.DownloadAssets;
import net.neoforged.gradle.common.runtime.tasks.ExtractNatives;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.dsl.userdev.runtime.definition.UserDevDefinition;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.userdev.runtime.specification.UserDevRuntimeSpecification;
import net.neoforged.gradle.userdev.runtime.tasks.ClasspathSerializer;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.MapProperty;
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
    public @NotNull FileCollection getAdditionalRecompileDependencies() {
        return neoformRuntimeDefinition.getAdditionalRecompileDependencies();
    }

    @Override
    protected void buildRunInterpolationData(RunImpl run, @NotNull MapProperty<String, String> interpolationData) {
        neoformRuntimeDefinition.buildRunInterpolationData(run, interpolationData);

        if (!userdevConfiguration.getFeatures().isPresent() ||
            !userdevConfiguration.getFeatures().get().getIsNoLegacyClasspath().getOrElse(false)
        ) {
            if (userdevConfiguration.getModules() != null && !userdevConfiguration.getModules().get().isEmpty()) {
                final Configuration modulesCfg = ConfigurationUtils
                    .temporaryUnhandledConfiguration(
                        getSpecification().getProject().getConfigurations(),
                        String.format("moduleResolverForgeUserDev%s", getSpecification().getVersionedName()),
                        userdevConfiguration.getModules().map(
                            modules -> modules.stream().map(
                                m -> getSpecification().getProject().getDependencies().create(m)
                            ).collect(Collectors.toList())
                        )
                    );

                interpolationData.put("modules", modulesCfg.getIncoming().getArtifacts().getResolvedArtifacts().map(artifacts -> artifacts.stream()
                    .map(ResolvedArtifactResult::getFile)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining(File.pathSeparator))));
            }

            final TaskProvider<ClasspathSerializer> minecraftClasspathSerializer = getSpecification().getProject().getTasks().register(
                RunsUtil.createNameFor("writeMinecraftClasspath", run),
                ClasspathSerializer.class,
                task -> {
                    final Configuration lcpConfiguration = ConfigurationUtils.temporaryConfiguration(getSpecification().getProject(),
                        RunsUtil.createNameFor("lcp", run));

                    ConfigurationUtils.extendsFrom(run.getProject(), lcpConfiguration, neoformRuntimeDefinition.getMinecraftDependenciesConfiguration());
                    ConfigurationUtils.extendsFrom(run.getProject(), lcpConfiguration, this.additionalUserDevDependencies);
                    ConfigurationUtils.extendsFrom(run.getProject(), lcpConfiguration, run.getDependencies().getRuntimeConfiguration());

                    //We depend on the configuration, this ensures that if we have dependencies with different versions in the
                    //dependency tree they are resolved to one version and are not added with different versions to the ConfigurableFileCollection
                    //on the input files.
                    //Additionally, we need to directly depend on the outputs file of the userdevClasspathElementProducer, we can not convert
                    //this to a dependency because it would be a transform of a configurable file collection that holds the outputs of the task
                    //which requires running that task, which would be a cyclic dependency.
                    //This is a workaround to ensure that the path to the userdev classpath element is resolved correctly.
                    //And added to the LCP, as we are now not transforming the CFC created from the outputs (we are not even creating a CFC)
                    task.getInputFiles().from(lcpConfiguration);
                    task.getInputFiles().from(this.userdevClasspathElementProducer.flatMap(WithOutput::getOutput));
                }
            );
            configureAssociatedTask(minecraftClasspathSerializer);
            interpolationData.put("minecraft_classpath_file", minecraftClasspathSerializer.flatMap(ClasspathSerializer::getTargetFile).map(RegularFile::getAsFile).map(File::getAbsolutePath));

            run.getPostSyncTasks().add(minecraftClasspathSerializer);
        }
    }

    @Override
    public void validateRun(final RunImpl run)
    {
        if (userdevConfiguration.getFeatures().isPresent() &&
            userdevConfiguration.getFeatures().get().getIsNoLegacyClasspath().getOrElse(false)
        )
        {
            ProjectUtils.afterEvaluate(getSpecification().getProject(), () -> {
                if (!run.getDependencies().getRuntimeConfiguration().getResolvedConfiguration().getResolvedArtifacts().isEmpty()) {
                    final IProblemReporter reporter = getSpecification().getProject().getExtensions().getByType(IProblemReporter.class);
                    reporter.reporting(problem -> problem
                            .id("deprecated-method", "Deprecated method")
                            .contextualLabel("Run.getDependencies().runtime() in run: %s".formatted(run.getName()))
                            .details("You are using a version of NeoForge which does not need run specific dependencies, they are loaded by default from your classpath.")
                            .solution("Remove your run specific dependencies block.")
                            .section("common-dep-run-specific-dependency-management"),
                        getSpecification().getProject().getLogger()
                    );
                }
            });
        }
    }

    @Override
    public Definition<?> getDelegate() {
        return neoformRuntimeDefinition;
    }

    public TaskProvider<? extends WithOutput> getUserdevClasspathElementProducer() {
        return userdevClasspathElementProducer;
    }

    public void setUserdevClasspathElementProducer(TaskProvider<? extends WithOutput> userdevClasspathElementProducer) {
        this.userdevClasspathElementProducer = userdevClasspathElementProducer;
    }
}
