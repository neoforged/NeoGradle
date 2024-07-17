package net.neoforged.gradle.common.runs.run;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunSourceSets;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.*;

public abstract class RunImpl implements ConfigurableDSLElement<Run>, Run {

    private final Project project;
    private final String name;
    private final ListProperty<RunType> runTypes;
    private final RunSourceSets modSources;
    private final RunSourceSets unitTestSources;

    private ListProperty<String> jvmArguments;
    private MapProperty<String, String> environmentVariables;
    private ListProperty<String> programArguments;
    private MapProperty<String, String> systemProperties;


    @Inject
    public RunImpl(final Project project, final String name) {
        this.project = project;
        this.name = name;
        this.modSources = project.getObjects().newInstance(RunSourceSetsImpl.class, project);
        this.unitTestSources = project.getObjects().newInstance(RunSourceSetsImpl.class, project);

        this.jvmArguments = this.project.getObjects().listProperty(String.class);
        this.environmentVariables = this.project.getObjects().mapProperty(String.class, String.class);
        this.programArguments = this.project.getObjects().listProperty(String.class);
        this.systemProperties = this.project.getObjects().mapProperty(String.class, String.class);
        this.runTypes = this.project.getObjects().listProperty(RunType.class);

        getIsSingleInstance().convention(true);
        getIsClient().convention(false);
        getIsServer().convention(false);
        getIsDataGenerator().convention(false);
        getIsGameTest().convention(false);
        getIsJUnit().convention(false);
        getShouldBuildAllProjects().convention(false);
        getDependencies().convention(project.getObjects().newInstance(DependencyHandlerImpl.class, project, String.format("RunRuntimeDependencies%s", StringCapitalizationUtils.capitalize(name))));

        getConfigureAutomatically().convention(true);
        getConfigureFromTypeWithName().convention(getConfigureAutomatically());
        getConfigureFromDependencies().convention(getConfigureAutomatically());
        
        getWorkingDirectory().convention(project.getLayout().getProjectDirectory().dir("runs").dir(getName()));
    }

    @Override
    public Project getProject() {
        return project;
    }
    @Override
    public final String getName() {
        return name;
    }

    @Override
    public MapProperty<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void overrideEnvironmentVariables(MapProperty<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public abstract Property<String> getMainClass();

    @Override
    public abstract Property<Boolean> getShouldBuildAllProjects();

    @Override
    public RunSourceSets getUnitTestSources() {
        return this.unitTestSources;
    }

    @Override
    public void unitTestSource(@NotNull final SourceSet sourceSet) {
        getUnitTestSources().add(sourceSet);
    }

    @Override
    public void unitTestSources(@NotNull final SourceSet... sourceSets) {
        getUnitTestSources().add(sourceSets);
    }

    @Override
    public void unitTestSources(@NotNull final Iterable<? extends SourceSet> sourceSets) {
        getUnitTestSources().add(sourceSets);
    }

    @Override
    public RunSourceSets getModSources() {
        return this.modSources;
    }

    @Override
    public void modSource(@NotNull final SourceSet sourceSet) {
        getModSources().add(sourceSet);
    }

    @Override
    public void modSources(@NotNull final SourceSet... sourceSets) {
        getModSources().add(sourceSets);
    }

    @Override
    public void modSources(@NotNull final Iterable<? extends SourceSet> sourceSets) {
        getModSources().add(sourceSets);
    }

    @Override
    public ListProperty<String> getProgramArguments() {
        return programArguments;
    }

    public void overrideProgramArguments(ListProperty<String> programArguments) {
        this.programArguments = programArguments;
    }

    @Override
    public ListProperty<String> getJvmArguments() {
        return jvmArguments;
    }

    public void overrideJvmArguments(final ListProperty<String> args) {
        this.jvmArguments = args;
    }

    @Override
    public MapProperty<String, String> getSystemProperties() {
        return systemProperties;
    }

    public void overrideSystemProperties(MapProperty<String, String> systemProperties) {
        this.systemProperties = systemProperties;
    }

    @Override
    public final void configure() {
        if (getConfigureFromTypeWithName().get()) {
            runTypes.add(getRunTypeByName(name));
        }

        getEnvironmentVariables().putAll(runTypes.flatMap(TransformerUtils.combineAllMaps(
                getProject(),
                String.class,
                String.class,
                RunType::getEnvironmentVariables
        )));
        getMainClass().convention(runTypes.flatMap(TransformerUtils.takeLast(getProject(), RunType::getMainClass)));
        getProgramArguments().addAll(runTypes.flatMap(TransformerUtils.combineAllLists(
                getProject(),
                String.class,
                RunType::getArguments
        )));
        getJvmArguments().addAll(runTypes.flatMap(TransformerUtils.combineAllLists(
                getProject(),
                String.class,
                RunType::getJvmArguments
        )));
        getIsSingleInstance().convention(runTypes.flatMap(TransformerUtils.takeLast(getProject(), RunType::getIsSingleInstance)));
        getSystemProperties().putAll(runTypes.flatMap(TransformerUtils.combineAllMaps(
                getProject(),
                String.class,
                String.class,
                RunType::getSystemProperties
        )));
        getIsClient().convention(runTypes.flatMap(TransformerUtils.takeLast(getProject(), RunType::getIsClient)));
        getIsServer().convention(runTypes.flatMap(TransformerUtils.takeLast(getProject(), RunType::getIsServer)));
        getIsDataGenerator().convention(runTypes.flatMap(TransformerUtils.takeLast(getProject(), RunType::getIsDataGenerator)));
        getIsGameTest().convention(runTypes.flatMap(TransformerUtils.takeLast(getProject(), RunType::getIsGameTest)));
        getIsJUnit().convention(runTypes.flatMap(TransformerUtils.takeLast(getProject(), RunType::getIsJUnit)));
        getClasspath().from(runTypes.map(TransformerUtils.combineFileCollections(
                getProject(),
                RunType::getClasspath
        )));
    }

    @Override
    public final void configure(final @NotNull String name) {
        getConfigureFromTypeWithName().set(false); // Don't re-configure
        runTypes.add(getRunTypeByName(name));
    }

    @Override
    public final void configure(final @NotNull RunType runType) {
        getConfigureFromTypeWithName().set(false); // Don't re-configure
        this.runTypes.add(project.provider(() -> runType));
    }

    @Override
    public void configure(@NotNull Provider<RunType> typeProvider) {
        getConfigureFromTypeWithName().set(false); // Don't re-configure
        this.runTypes.add(typeProvider);
    }

    @NotNull
    public List<String> realiseJvmArguments() {
        final List<String> args = new ArrayList<>(getJvmArguments().get());

        // This mirrors the logic found in Gradle itself, which also does not quote key nor value
        for (Map.Entry<String, String> entry : getSystemProperties().get().entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                args.add("-D" + entry.getKey() + "=" + entry.getValue());
            } else {
                args.add("-D" + entry.getKey());
            }
        }

        return args;
    }

    @SuppressWarnings("unchecked")
    private Provider<RunType> getRunTypeByName(String name) {
        NamedDomainObjectContainer<RunType> runTypes = (NamedDomainObjectContainer<RunType>) project.getExtensions()
                .getByName(RunsConstants.Extensions.RUN_TYPES);

        return project.provider(() -> {
            if (runTypes.getNames().contains(name)) {
                return runTypes.getByName(name);
            } else {
                throw new GradleException("Could not find run type " + name + ". Available run types: " +
                        runTypes.getNames());
            }
        });
    }
}
