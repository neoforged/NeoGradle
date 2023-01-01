package net.minecraftforge.gradle.runs.run;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.runs.run.Run;
import net.minecraftforge.gradle.dsl.runs.type.Type;
import net.minecraftforge.gradle.dsl.runs.type.Types;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

public abstract class RunImpl extends ConfigurableObject<Run> implements Run {

    private final Project project;
    private final String name;

    public RunImpl(final Project project, final String name) {
        this.project = project;
        this.name = name;

        getWorkingDirectory().convention(project.getLayout().getProjectDirectory().dir("run"));
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
    public abstract MapProperty<String, String> getEnvironmentVariables();

    @Override
    public abstract Property<String> getMainClass();

    @Override
    public abstract Property<Boolean> getShouldBuildAllProjects();

    @Override
    public abstract ListProperty<String> getProgramArguments();

    @Override
    public abstract ListProperty<String> getJvmArguments();

    @Override
    public abstract Property<Boolean> getIsSingleInstance();

    @Override
    public abstract MapProperty<String, String> getSystemProperties();

    @Override
    public abstract DirectoryProperty getWorkingDirectory();

    @Override
    public abstract Property<Boolean> getIsClient();

    @Override
    public abstract ListProperty<TaskProvider<? extends Jar>> getRunningJars();

    @Override
    @NotNull
    public final void configure() {
        configure(getName());
    }

    @Override
    @NotNull
    public final void configure(final String name) {
        final Types types = getProject().getExtensions().getByType(Types.class);
        configure(types.getByName(name));
    }

    @Override
    @NotNull
    public final void configure(final Type spec) {
        getEnvironmentVariables().convention(spec.getEnvironmentVariables());
        getMainClass().convention(spec.getMainClass());
        getProgramArguments().convention(spec.getArguments());
        getJvmArguments().convention(spec.getJvmArguments());
        getIsSingleInstance().convention(spec.getIsSingleInstance());
        getSystemProperties().convention(spec.getSystemProperties());
        getIsClient().convention(spec.getIsClient());
    }
}
