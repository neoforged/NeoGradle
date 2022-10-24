/*
 * ForgeGradle
 * Copyright (C) 2018 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package net.minecraftforge.gradle.runs;

import groovy.lang.GroovyObjectSupport;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import net.minecraftforge.gradle.common.util.*;
import net.minecraftforge.gradle.runs.config.RunConfigurationSpec;
import net.minecraftforge.gradle.runs.util.RunsConstants;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;

import java.util.List;
import java.util.stream.Collectors;

public abstract class RunConfiguration extends GroovyObjectSupport implements IConfigurableObject<RunConfiguration> {

    public static final String RUNS_GROUP = "ForgeGradle runs";

    private final Project project;
    private final Provider<String> name;
    private final NamedDomainObjectContainer<ModConfig> mods;
    private final ConfigurableFileCollection minecraftClasspath;

    public RunConfiguration(final Project project, final String name) {
        this.project = project;

        this.name = getProject().provider(() -> name);
        this.mods = getProject().container(ModConfig.class, modName -> getProject().getObjects().newInstance(ModConfig.class, getProject(), modName));
        this.minecraftClasspath = getProject().files();

        getTaskName().convention(getName().map(runConfigName -> {
            final String conventionTaskName = runConfigName.replaceAll("[^a-zA-Z0-9\\-_]", "");
            if (conventionTaskName.startsWith("run")) {
                return conventionTaskName;
            }

            return "run" + Utils.capitalize(conventionTaskName);
        }));
        getUniqueFileName().convention(getTaskName().map(taskName -> FileUtils.buildFileNameForTask(getProject(), taskName)));
        getIdeaModuleName().convention(project.provider(() -> String.format("%s.main", getProject().getName().replace(' ', '_'))));
        getWorkingDirectory().convention(project.getLayout().getProjectDirectory().dir("run"));
    }

    public Project getProject() {
        return project;
    }
    public final Provider<String> getName() {
        return name;
    }
    public abstract Property<String> getTaskName();
    public abstract Property<String> getUniqueFileName();
    public final Provider<String> getUniqueName() { return getUniqueFileName().map(uniqueFileName -> uniqueFileName.replace("_", " ")); }
    public abstract MapProperty<String, Object> getEnvironmentVariables();
    public abstract Property<String> getMainClass();
    public abstract Property<Boolean> getShouldBuildAllProjects();
    public abstract ListProperty<String> getProgramArguments();
    public abstract ListProperty<String> getJvmArguments();
    public abstract Property<Boolean> getShouldBeSingleInstanceOnly();
    public abstract MapProperty<String, String> getSystemProperties();
    public abstract Property<String> getIdeaModuleName();
    public abstract DirectoryProperty getWorkingDirectory();
    public abstract Property<Boolean> getIsClient();
    public final NamedDomainObjectContainer<ModConfig> getMods() {
        return mods;
    }
    public final ConfigurableFileCollection getMinecraftClasspath() {
        return minecraftClasspath;
    }
    public final Provider<List<TaskProvider<Task>>> getDependentTasks() {
        return getAllSourceSets().map(sourceSets -> sourceSets.stream().map(sourceSet -> project.getTasks().named(sourceSet.getClassesTaskName())).collect(Collectors.toList()));
    }
    public final Provider<List<SourceSet>> getAllSourceSets() {
        return ProviderUtils.getNamedCollectionEntriesAsProvider(project, getMods())
                .map(modConfigs -> modConfigs.stream().map(ModConfig::getSourceSets).flatMap(provider -> provider.get().stream()).collect(Collectors.toList()));
    }
    @SuppressWarnings("unchecked")
    public final NamedDomainObjectContainer<RunConfigurationSpec> getSpecifications() {
        return (NamedDomainObjectContainer<RunConfigurationSpec>) getProject().getExtensions().getByName(RunsConstants.Extensions.RUN_SPECS);
    }
    public final ConfigurableFileCollection getRuntimeClasspath() {
        return getProject().files(getMinecraftClasspath(), getProject().getConfigurations().getByName("runtimeClasspath"));
    }


    @NotNull
    public final RunConfiguration configure() {
        return configure(getName().get());
    }

    @NotNull
    public final RunConfiguration configure(final String name) {
        final RunConfigurationSpec runConfigurationSpec = getSpecifications().getByName(name);
        return configure(runConfigurationSpec);
    }

    @NotNull
    public final RunConfiguration configure(final RunConfigurationSpec spec) {
        getEnvironmentVariables().convention(spec.getEnv());
        getMainClass().convention(spec.getMain());
        getProgramArguments().convention(spec.getArgs());
        getJvmArguments().convention(spec.getJvmArgs());
        getShouldBeSingleInstanceOnly().convention(spec.getSingleInstance());
        getSystemProperties().convention(spec.getProps());
        getIsClient().convention(spec.getClient());

        return this;
    }
}
