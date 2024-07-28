package net.neoforged.gradle.common.runs.run;

import com.google.common.collect.Multimap;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.extensions.NeoGradleProblemReporter;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.runs.run.*;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.runs.type.RunTypeManager;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.*;

public abstract class RunImpl implements ConfigurableDSLElement<Run>, Run {

    private final Project project;
    private final String name;
    private final ListProperty<RunType> runTypes;
    private final RunSourceSets modSources;
    private final RunSourceSets unitTestSources;
    private final RunTestScope testScope;
    private final RunRenderDocOptions renderDocOptions;
    private final RunDevLoginOptions devLoginOptions;

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
        this.testScope = project.getObjects().newInstance(RunTestScopeImpl.class, project);
        this.renderDocOptions = project.getObjects().newInstance(RunRenderDocOptionsImpl.class, project);
        this.devLoginOptions = project.getObjects().newInstance(RunDevLoginOptionsImpl.class, project, this);

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

        getRuntimeClasspath().from(
                getModSources().all().map(Multimap::values)
                        .map(sourcesSets -> sourcesSets.stream().map(SourceSet::getRuntimeClasspath).toList())
        );
        getTestRuntimeClasspath().from(getRuntimeClasspath());
        getTestRuntimeClasspath().from(
                getUnitTestSources().all().map(Multimap::values)
                        .map(sourcesSets -> sourcesSets.stream().map(SourceSet::getRuntimeClasspath).toList())
        );
        getCompileClasspath().from(
                getModSources().all().map(Multimap::values)
                        .map(sourcesSets -> sourcesSets.stream().map(SourceSet::getCompileClasspath).toList())
        );
        getTestCompileClasspath().from(getCompileClasspath());
        getTestCompileClasspath().from(
                getUnitTestSources().all().map(Multimap::values)
                        .map(sourcesSets -> sourcesSets.stream().map(SourceSet::getCompileClasspath).toList())
        );
        getSdkClasspath().from(
                getModSources().all().map(Multimap::values)
                        .map(sourcesSets -> sourcesSets.stream().map(ConfigurationUtils::getSdkConfiguration).toList())
        );
        getSdkClasspath().from(
                getUnitTestSources().all().map(Multimap::values)
                        .map(sourcesSets -> sourcesSets.stream().map(ConfigurationUtils::getSdkConfiguration).toList())
        );
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
    public RunRenderDocOptions getRenderDoc() {
        return renderDocOptions;
    }

    @Override
    public RunDevLoginOptions getDevLogin() {
        return devLoginOptions;
    }

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

    private Provider<Set<FileSystemLocation>> getLooselyCoupledConfigurableFileCollectionElements(final ConfigurableFileCollection collection) {
        //This is needed because the returned providers should be transformable. Which they are not by default.
        //You can call get() on the provider returned by getElements, without any issue directly, regardless of task evaluation state.
        //However, if you then transform the provider and call get() on the returned provider then a task state check is additionally added, and
        //The execution crashes, even though we are not interested in the execution, just the locations.
        return project.provider(() -> collection.getElements().get());
    }

    @Override
    public Provider<Set<FileSystemLocation>> getRuntimeClasspathElements() {
        return getLooselyCoupledConfigurableFileCollectionElements(getRuntimeClasspath());
    }

    @Override
    public Provider<Set<FileSystemLocation>> getTestRuntimeClasspathElements() {
        return getLooselyCoupledConfigurableFileCollectionElements(getTestRuntimeClasspath());
    }

    @Override
    public Provider<Set<FileSystemLocation>> getCompileClasspathElements() {
        return getLooselyCoupledConfigurableFileCollectionElements(getCompileClasspath());
    }

    @Override
    public Provider<Set<FileSystemLocation>> getTestCompileClasspathElements() {
        return getLooselyCoupledConfigurableFileCollectionElements(getTestCompileClasspath());
    }

    @Override
    public Provider<Set<FileSystemLocation>> getSdkClasspathElements() {
        return getLooselyCoupledConfigurableFileCollectionElements(getSdkClasspath());
    }

    @Override
    public void runType(@NotNull String string) {
        configure(string);
    }

    @Override
    public RunTestScope getTestScope() {
        return testScope;
    }

    @Override
    public final void configure() {
        if (getConfigureFromTypeWithName().get()) {
            runTypes.addAll(getRunTypesByName(name));
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
        getRuntimeClasspath().from(runTypes.map(TransformerUtils.combineFileCollections(
                getProject(),
                RunType::getClasspath
        )));

        final Set<SourceSet> unconfiguredSourceSets = new HashSet<>();
        final Set<CommonRuntimeDefinition<?>> configuredDefinitions = new HashSet<>();

        getModSources().whenSourceSetAdded(sourceSet -> {
            // Only configure the run if the source set is from the same project
            if (SourceSetUtils.getProject(sourceSet) != getProject())
                return;

            try {
                final Optional<CommonRuntimeDefinition<?>> definition = TaskDependencyUtils.findRuntimeDefinition(sourceSet);
                definition.ifPresentOrElse(def -> {
                    if (configuredDefinitions.add(def)) {
                        def.configureRun(this);
                    }
                }, () -> unconfiguredSourceSets.add(sourceSet));
            } catch (MultipleDefinitionsFoundException e) {
                final NeoGradleProblemReporter reporter = project.getExtensions().getByType(NeoGradleProblemReporter.class);
                throw reporter.throwing(problem -> problem
                        .id("multiple-definitions-found", "Multiple runtime definitions found")
                        .contextualLabel("Run: " + this.getName())
                        .solution("Ensure only one SDK definition is present for the source set")
                        .details("There are multiple runtime definitions found for the source set: " + sourceSet.getName())
                        .section("common-runs-configuration-runs")
                );
            }
        });

        final DependencyReplacement replacementLogic = project.getExtensions().getByType(DependencyReplacement.class);
        replacementLogic.whenDependencyReplaced((virtualDependency, targetConfiguration, originalDependency) -> {
            if (unconfiguredSourceSets.isEmpty()) {
                return;
            }

            for (final Iterator<SourceSet> iterator = unconfiguredSourceSets.iterator(); iterator.hasNext(); ) {
                SourceSet unconfiguredSourceSet = iterator.next();
                // Only configure the run if the source set is from the same project
                if (SourceSetUtils.getProject(unconfiguredSourceSet) != getProject())
                    return;

                try {
                    final Optional<CommonRuntimeDefinition<?>> definition = TaskDependencyUtils.findRuntimeDefinition(unconfiguredSourceSet);
                    definition.ifPresent(def -> {
                        if (configuredDefinitions.add(def)) {
                            def.configureRun(this);
                        }
                        iterator.remove();
                    });
                } catch (MultipleDefinitionsFoundException e) {
                    throw new RuntimeException("Failed to configure run: " + getName() + " there are multiple runtime definitions found for the source set: " + unconfiguredSourceSet.getName(), e);
                }
            }
        });
    }

    @Override
    public final void configure(final @NotNull String name) {
        getConfigureFromTypeWithName().set(false); // Don't re-configure
        runTypes.addAll(getRunTypesByName(name));
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

    private Provider<List<RunType>> getRunTypesByName(String name) {
        RunTypeManager runTypes = project.getExtensions().getByType(RunTypeManager.class);

        return project.provider(() -> {
            if (runTypes.getNames().contains(name)) {
                return List.of(runTypes.getByName(name));
            } else {
                return null;
            }
        }).orElse(
                TransformerUtils.ifTrue(getConfigureFromDependencies(),
                        getSdkClasspathElements()
                                .map(files -> files.stream()
                                        .map(FileSystemLocation::getAsFile)
                                        .map(runTypes::parse)
                                        .flatMap(Collection::stream)
                                        .filter(runType -> runType.getName().equals(name))
                                        .toList()
                                ))).map(types -> {
            if (types.isEmpty()) {
                final NeoGradleProblemReporter reporter = project.getExtensions().getByType(NeoGradleProblemReporter.class);
                reporter.reporting(problem -> problem
                        .id("run-type-not-found", "Run type not found")
                        .contextualLabel("The run type '%s' was not found".formatted(name))
                        .severity(org.gradle.api.problems.Severity.ERROR)
                        .solution("Ensure the run type is defined in the run or a dependency")
                );
            }
            return types;
        });
    }
}
