package net.neoforged.gradle.common.runs.run;

import com.google.common.collect.Multimap;
import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.extensions.problems.IProblemReporter;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.common.util.TaskDependencyUtils;
import net.neoforged.gradle.common.util.exceptions.MultipleDefinitionsFoundException;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.runs.DevLogin;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.runs.RenderDoc;
import net.neoforged.gradle.dsl.common.runs.RunSpecification;
import net.neoforged.gradle.dsl.common.runs.run.*;
import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.runs.type.RunTypeManager;
import net.neoforged.gradle.util.StringCapitalizationUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.function.Function;

public abstract class RunImpl implements ConfigurableDSLElement<Run>, Run {

    private final Project project;
    private final String name;
    private final ListProperty<RunSpecification> rawSpecifications;
    private final ListProperty<RunSpecification> specifications;
    private final RunSourceSets modSources;
    private final RunSourceSets unitTestSources;
    private final RunTestScope testScope;
    private final RunRenderDocOptions renderDocOptions;
    private final RunDevLoginOptions devLoginOptions;
    private final DependencyHandler dependencies;

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
        this.renderDocOptions = project.getObjects().newInstance(RunRenderDocOptionsImpl.class, project, this);
        this.devLoginOptions = project.getObjects().newInstance(RunDevLoginOptionsImpl.class, project, this);
        this.dependencies = project.getObjects().newInstance(DependencyHandlerImpl.class, project, String.format("RunDependencies%s", StringCapitalizationUtils.capitalize(name)));

        this.jvmArguments = this.project.getObjects().listProperty(String.class);
        this.environmentVariables = this.project.getObjects().mapProperty(String.class, String.class);
        this.programArguments = this.project.getObjects().listProperty(String.class);
        this.systemProperties = this.project.getObjects().mapProperty(String.class, String.class);

        this.rawSpecifications = this.project.getObjects().listProperty(RunSpecification.class);
        this.specifications = this.project.getObjects().listProperty(RunSpecification.class);
        this.specifications.addAll(rawSpecifications);

        getIsSingleInstance().convention(true);
        getIsClient().convention(false);
        getIsServer().convention(false);
        getIsDataGenerator().convention(false);
        getIsGameTest().convention(false);
        getIsJUnit().convention(false);

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

        getShouldExportToIDE().convention(true);
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
    public DependencyHandler getDependencies() {
        return dependencies;
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
    public ListProperty<String> getArguments() {
        return programArguments;
    }

    public void overrideArguments(ListProperty<String> arguments) {
        this.programArguments = arguments;
    }

    @Deprecated
    public ListProperty<String> getProgramArguments() {
        getProject().getExtensions().getByType(IProblemReporter.class)
                .reporting(problem -> problem
                                .id("deprecated-method", "Deprecated method")
                                .contextualLabel("Run.getProgramArguments()")
                                .details("The method getProgramArguments() is deprecated and will be removed in the future")
                                .solution("Use getArguments() instead of getProgramArguments()")
                                .section("common-runs-configuration-types-configure-by-type"),
                        getProject().getLogger()
                );

        return programArguments;
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
    public void runType(@NotNull String name) {
        getConfigureFromTypeWithName().set(false); // Don't re-configure
        rawSpecifications.addAll(getRunTypesByName(name));
    }

    @Override
    public void run(@NotNull String name) {
        getConfigureFromTypeWithName().set(false); // Don't re-configure
        rawSpecifications.addAll(getRunByName(name));
    }

    @Override
    public RunTestScope getTestScope() {
        return testScope;
    }

    @Override
    public final void configure() {
        potentiallyAddRunTypeByName();
        potentiallyAddRunTemplateFromType();
        configureRunSpecification();
        configureFromSDKs();
        configureFromRuns();
    }

    private void potentiallyAddRunTemplateFromType() {
        specifications.addAll(
                rawSpecifications.map(l -> l.stream().filter(RunType.class::isInstance).map(RunType.class::cast)
                                .map(RunType::getRunTemplate)
                                .filter(Objects::nonNull)
                                .toList())
        );
    }

    private void configureFromRuns() {
        Provider<List<Run>> runSpecifications = specifications.map(l -> l.stream().filter(Run.class::isInstance).map(Run.class::cast).toList());

        //Properties of the run
        getWorkingDirectory().convention(
                TransformerUtils.defaulted(
                        runSpecifications.flatMap(
                                TransformerUtils.takeLast(project, Run::getWorkingDirectory)
                        ),
                        project.getLayout().getProjectDirectory().dir("runs").dir(getName())
                )
        );

        final RenderDoc renderDoc = project.getExtensions().getByType(Subsystems.class).getConventions().getRuns().getRenderDoc();
        //Properties of the renderdoc integration
        getRenderDoc().getEnabled().convention(
                TransformerUtils.lazyDefaulted(
                        runSpecifications.flatMap(
                                TransformerUtils.takeLast(project, run -> run.getRenderDoc().getEnabled())
                        ),
                        renderDoc.getConventionForRun().zip(getIsClient(), (conventionForRun, isClient) -> conventionForRun && isClient)
                )
        );

        //Properties of the dev login integration
        final DevLogin devLogin = project.getExtensions().getByType(Subsystems.class).getConventions().getRuns().getDevLogin();
        getDevLogin().getIsEnabled().convention(
                TransformerUtils.lazyDefaulted(
                        runSpecifications.flatMap(
                                TransformerUtils.takeLast(project, run -> run.getDevLogin().getIsEnabled())
                        ),
                        devLogin.getConventionForRun().zip(getIsClient(), (conventionForRun, isClient) -> conventionForRun && isClient)
                )
        );
        getDevLogin().getProfile().convention(
                runSpecifications.flatMap(
                        TransformerUtils.takeLast(project, run -> run.getDevLogin().getProfile())
                )
        );

        //ModSources
        getModSources().addAllLater(
                runSpecifications.flatMap(
                        TransformerUtils.combineAllMultiMaps(
                                project,
                                String.class,
                                SourceSet.class,
                                run -> run.getModSources().all()
                        )
                )
        );

        //UnitTestSources
        getUnitTestSources().addAllLater(
                runSpecifications.flatMap(
                        TransformerUtils.combineAllMultiMaps(
                                project,
                                String.class,
                                SourceSet.class,
                                run -> run.getUnitTestSources().all()
                        )
                )
        );

        //Properties of the test scope
        getTestScope().getPackageName().convention(
                runSpecifications.flatMap(
                        TransformerUtils.takeLast(project, run -> run.getTestScope().getPackageName())
                )
        );
        getTestScope().getDirectory().convention(
                runSpecifications.flatMap(
                        TransformerUtils.takeLast(project, run -> run.getTestScope().getDirectory())
                )
        );
        getTestScope().getPattern().convention(
                TransformerUtils.lazyDefaulted(
                        runSpecifications.flatMap(
                                TransformerUtils.takeLast(project, run -> run.getTestScope().getPattern())
                        ),
                        project.provider(() -> {
                            if (getTestScope().getPackageName().orElse(getTestScope().getDirectory().map(Directory::getAsFile).map(File::getName))
                                    .orElse(getTestScope().getClassName())
                                    .orElse(getTestScope().getMethod())
                                    .orElse(getTestScope().getCategory())
                                    .getOrNull() == null) {
                                return RunTestScopeImpl.DEFAULT_PATTERN;
                            }

                            return null;
                        })
                )
        );
        getTestScope().getClassName().convention(
                runSpecifications.flatMap(
                        TransformerUtils.takeLast(project, run -> run.getTestScope().getClassName())
                )
        );
        getTestScope().getMethod().convention(
                runSpecifications.flatMap(
                        TransformerUtils.takeLast(project, run -> run.getTestScope().getMethod())
                )
        );
        getTestScope().getCategory().convention(
                runSpecifications.flatMap(
                        TransformerUtils.takeLast(project, run -> run.getTestScope().getCategory())
                )
        );

        //Dependencies
        getDependencies().getRuntime().bundle(
                runSpecifications.flatMap(
                        TransformerUtils.combineAllSets(
                                project,
                                Dependency.class,
                                run -> run.getDependencies().getRuntime().getDependencies()
                        )
                )
        );

        //Task dependencies
        getDependsOn().addAll(
                runSpecifications.flatMap(
                        TransformerUtils.combineAllSets(
                                project,
                                Task.class,
                                Run::getDependsOn
                        )
                )
        );

        //Pre-sync tasks
        getPostSyncTasks().addAll(
                runSpecifications.flatMap(
                        TransformerUtils.combineAllSets(
                                project,
                                Task.class,
                                Run::getPostSyncTasks
                        )
                )
        );

        //Exporting to IDEs
        getShouldExportToIDE().convention(
                TransformerUtils.defaulted(
                        runSpecifications.flatMap(
                                TransformerUtils.takeLast(project, Run::getShouldExportToIDE)
                        ),
                        true
                )
        );
    }

    private void configureFromSDKs() {
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
                final IProblemReporter reporter = project.getExtensions().getByType(IProblemReporter.class);
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

    private void potentiallyAddRunTypeByName() {
        if (getConfigureFromTypeWithName().get()) {
            rawSpecifications.addAll(getRunTypesByName(name));
        }
    }

    private void configureRunSpecification() {
        getEnvironmentVariables().putAll(specifications.flatMap(TransformerUtils.combineAllMaps(
                getProject(),
                String.class,
                String.class,
                RunSpecification::getEnvironmentVariables
        )));
        getMainClass().convention(specifications.flatMap(TransformerUtils.takeLast(getProject(), RunSpecification::getMainClass)));
        getArguments().addAll(specifications.flatMap(TransformerUtils.combineAllLists(
                getProject(),
                String.class,
                RunSpecification::getArguments
        )));
        getJvmArguments().addAll(specifications.flatMap(TransformerUtils.combineAllLists(
                getProject(),
                String.class,
                RunSpecification::getJvmArguments
        )));
        getIsSingleInstance().convention(
                TransformerUtils.defaulted(
                        specifications.flatMap(TransformerUtils.takeLast(getProject(), RunSpecification::getIsSingleInstance)),
                        true
                )
        );
        getSystemProperties().putAll(specifications.flatMap(TransformerUtils.combineAllMaps(
                getProject(),
                String.class,
                String.class,
                RunSpecification::getSystemProperties
        )));
        getIsClient().convention(
                TransformerUtils.defaulted(
                        specifications.flatMap(TransformerUtils.takeLast(getProject(), RunSpecification::getIsClient)),
                        false
                )
        );
        getIsServer().convention(
                TransformerUtils.defaulted(
                        specifications.flatMap(TransformerUtils.takeLast(getProject(), RunSpecification::getIsServer)),
                        false
                )
        );
        getIsDataGenerator().convention(
                TransformerUtils.defaulted(
                        specifications.flatMap(TransformerUtils.takeLast(getProject(), RunSpecification::getIsDataGenerator)),
                        false
                )
        );
        getIsGameTest().convention(
                TransformerUtils.defaulted(
                        specifications.flatMap(TransformerUtils.takeLast(getProject(), RunSpecification::getIsGameTest)),
                        false
                )
        );
        getIsJUnit().convention(
                TransformerUtils.defaulted(
                        specifications.flatMap(TransformerUtils.takeLast(getProject(), RunSpecification::getIsJUnit)),
                        false
                )
        );
        getRuntimeClasspath().from(specifications.map(TransformerUtils.combineFileCollections(
                getProject(),
                RunSpecification::getClasspath
        )));
    }

    @Deprecated
    @Override
    public final void configure(final @NotNull String name) {
        getProject().getExtensions().getByType(IProblemReporter.class)
                .reporting(problem -> problem
                                .id("deprecated-method", "Deprecated method")
                                .contextualLabel("Run.configure(String)")
                                .details("The method configure(String) is deprecated and will be removed in the future")
                                .solution("Use Run.runType(String) or Run.run(String) instead of Run.configure(String) to indicate from what the run should be configured")
                                .section("common-runs-configuration-types-configure-by-type"),
                        getProject().getLogger()
                );

        getConfigureFromTypeWithName().set(false); // Don't re-configure
        rawSpecifications.addAll(getRunTypesByName(name));
    }

    @Override
    public final void configure(final @NotNull RunSpecification runType) {
        getConfigureFromTypeWithName().set(false); // Don't re-configure
        this.rawSpecifications.add(project.provider(() -> runType));
    }

    @Override
    public void configure(@NotNull Provider<? extends RunSpecification> typeProvider) {
        getConfigureFromTypeWithName().set(false); // Don't re-configure
        this.rawSpecifications.add(typeProvider);
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
                })
                .orElse(
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
                        final IProblemReporter reporter = project.getExtensions().getByType(IProblemReporter.class);
                        throw reporter.throwing(problem -> problem
                                .id("run-type-not-found", "Run type not found")
                                .contextualLabel("The run type '%s' was not found".formatted(name))
                                .solution("Ensure the run type is defined in the run or a dependency")
                                .section("common-runs-configuration-types-configure-by-type")
                        );
                    }
                    return types;
                });
    }

    private Provider<List<Run>> getRunByName(String name) {
        RunManager runTypes = project.getExtensions().getByType(RunManager.class);

        return project.provider(() -> {
                    if (runTypes.getNames().contains(name)) {
                        return List.of(runTypes.getByName(name));
                    } else {
                        return null;
                    }
                })
                .orElse(List.of());
    }
}
