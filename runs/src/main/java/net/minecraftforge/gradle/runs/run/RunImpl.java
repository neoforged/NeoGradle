package net.minecraftforge.gradle.runs.run;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.gradle.util.ConfigurableObject;
import net.minecraftforge.gradle.base.util.ProjectUtils;
import net.minecraftforge.gradle.dsl.runs.run.DependencyHandler;
import net.minecraftforge.gradle.dsl.runs.run.Run;
import net.minecraftforge.gradle.dsl.runs.type.Type;
import net.minecraftforge.gradle.dsl.runs.type.Types;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;

public abstract class RunImpl extends ConfigurableObject<Run> implements Run {

    private final Project project;
    private final String name;

    private ListProperty<String> jvmArguments;
    private MapProperty<String, String> environmentVariables;
    private ListProperty<String> programArguments;
    private MapProperty<String, String> systemProperties;

    @Inject
    public RunImpl(final Project project, final String name) {
        this.project = project;
        this.name = name;

        getWorkingDirectory().convention(project.getLayout().getProjectDirectory().dir("run"));

        this.jvmArguments = this.project.getObjects().listProperty(String.class);
        this.environmentVariables = this.project.getObjects().mapProperty(String.class, String.class);
        this.programArguments = this.project.getObjects().listProperty(String.class);
        this.systemProperties = this.project.getObjects().mapProperty(String.class, String.class);

        getIsSingleInstance().convention(true);
        getIsClient().convention(true);
        getShouldBuildAllProjects().convention(false);
        getDependencies().convention(project.getObjects().newInstance(DependencyHandlerImpl.class, project));

        getConfigureAutomatically().convention(true);
        getConfigureFromTypeWithName().convention(getConfigureAutomatically());
        getConfigureFromDependencies().convention(getConfigureAutomatically());
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
    public abstract Property<Boolean> getIsSingleInstance();

    @Override
    public MapProperty<String, String> getSystemProperties() {
        return systemProperties;
    }

    public void overrideSystemProperties(MapProperty<String, String> systemProperties) {
        this.systemProperties = systemProperties;
    }

    @Override
    public abstract DirectoryProperty getWorkingDirectory();

    @Override
    public abstract Property<Boolean> getIsClient();

    @Override
    public abstract ListProperty<SourceSet> getModSources();

    @Override
    public abstract ConfigurableFileCollection getClasspath();

    @Override
    public abstract Property<DependencyHandler> getDependencies();

    @Override
    public abstract Property<Boolean> getConfigureAutomatically();

    @Override
    public abstract Property<Boolean> getConfigureFromTypeWithName();

    @Override
    public abstract Property<Boolean> getConfigureFromDependencies();

    @Override
    @NotNull
    public final void configure() {
        configure(getName());
    }

    @Override
    @NotNull
    public final void configure(final String name) {
        ProjectUtils.afterEvaluate(getProject(), () -> {
            final Types types = getProject().getExtensions().getByType(Types.class);
            if (types.getNames().contains(name)) {
                configureInternally(types.getByName(name));
            }
        });
    }

    @Override
    @NotNull
    public final void configure(final Type type) {
        ProjectUtils.afterEvaluate(getProject(), () -> {
            configureInternally(type);
        });
    }

    @NotNull
    public void configureInternally(final Type spec) {
        getEnvironmentVariables().convention(spec.getEnvironmentVariables());
        getMainClass().convention(spec.getMainClass());
        getProgramArguments().convention(spec.getArguments());
        getJvmArguments().convention(spec.getJvmArguments());
        getIsSingleInstance().convention(spec.getIsSingleInstance());
        getSystemProperties().convention(spec.getSystemProperties());
        getIsClient().convention(spec.getIsClient());
        getClasspath().from(spec.getClasspath());
    }
}
