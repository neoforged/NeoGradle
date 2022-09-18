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

package net.minecraftforge.gradle.common.util;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class RunConfig extends GroovyObjectSupport implements Serializable {

    public static final String RUNS_GROUP = "ForgeGradle runs";

    private static final String MCP_CLIENT_MAIN = "mcp.client.Start";
    private static final String MC_CLIENT_MAIN = "net.minecraft.client.main.Main";

    private static final long serialVersionUID = 1L;

    private transient final Project project;
    private transient NamedDomainObjectContainer<ModConfig> mods;

    private final Provider<String> name;
    private final Provider<String> uniqueName;

    private Boolean singleInstance = null;

    private String taskName;
    private String main;
    private String ideaModule;
    private String workDir;

    private List<SourceSet> sources;
    private List<RunConfig> parents;
    private List<RunConfig> children;
    private List<String> args, jvmArgs;
    private boolean forceExit = true;
    private Boolean client; // so we can have it null
    private Boolean inheritArgs;
    private Boolean inheritJvmArgs;
    private boolean buildAllProjects;

    private Map<String, String> env, props, tokens;
    private Map<String, Supplier<String>> lazyTokens;
    private Provider<List<Object>> runtimeProgramArguments;
    private Provider<List<Object>> runtimeJvmArguments;
    private Provider<Map<String, Object>> runtimeEnvironmentVariables;
    private Provider<Map<String, Object>> runtimeSystemProperties;

    private Provider<Set<ModConfig>> runtimeModConfigurations;

    public RunConfig(final Project project, final String name) {
        this.project = project;

        this.name = project.provider(() -> name);
        this.uniqueName = project.provider(this.getUniqueFileName().map(uniqueFileName -> uniqueFileName.replace("_", " "))::get);

        this.mods = project.container(ModConfig.class, modName -> new ModConfig(project, modName));

        this.runtimeProgramArguments = getProgramArguments();
        this.runtimeJvmArguments = getJvmArguments();
        this.runtimeEnvironmentVariables = getEnvironmentVariables();
        this.runtimeSystemProperties = getSystemProperties();
        this.runtimeModConfigurations = project.project(() -> getMods());

        getTaskName().convention(getName().map(runConfigName -> {
            final String conventionTaskName = runConfigName.replaceAll("[^a-zA-Z0-9\\-_]", "");
            if (conventionTaskName.startsWith("run")) {
                return conventionTaskName;
            }

            return "run" + Utils.capitalize(conventionTaskName);
        }));
        getUniqueFileName().convention(getTaskName().map(taskName -> {
            return project.getPath().length() > 1 ? String.join("_", String.join("_", project.getPath().substring(1).split(":")), taskName) : taskName;
        }));

    }

    public final Provider<String> getName() {
        return name;
    }

    public abstract Property<String> getTaskName();

    public abstract Property<String> getUniqueFileName();

    public final Provider<String> getUniqueName() { return uniqueName; }

    public abstract Property<Boolean> getShouldInheritEnvironmentVariables();

    public abstract MapProperty<String, Object> getEnvironmentVariables();

    public final Provider<Map<String, Object>> getRuntimeEnvironmentVariables() {
        return this.runtimeEnvironmentVariables;
    }

    public abstract Property<String> getMainClass();
    public abstract Property<Boolean> getShouldInheritProgramArguments();

    public abstract Property<Boolean> getShouldInheritJvmArguments();

    public abstract Property<Boolean> getShouldBuildAllProjects();

    public abstract ListProperty<Object> getProgramArguments();

    public final Provider<List<Object>> getRuntimeProgramArguments() {
        return project.provider(() -> this.runtimeProgramArguments.get());
    }

    public abstract ListProperty<Object> getJvmArguments();

    public final Provider<List<Object>> getRuntimeJvmArguments() {
        return project.provider(() -> this.runtimeJvmArguments.get());
    }

    public abstract Property<Boolean> getShouldBeSingleInstanceOnly();

    public abstract Property<Boolean> getShouldInheritSystemProperties();

    public abstract MapProperty<String, Object> getSystemProperties();

    public final Provider<Map<String, Object>> getRuntimeSystemProperties() {
        return project.provider(() -> this.runtimeSystemProperties.get());
    }

    public abstract Property<String> getIdeaModuleName(); // project.getName().replace(' ', '_') + ".main";

    public abstract DirectoryProperty getWorkingDirectory(); // project.file("run").getAbsolutePath();

    public abstract Property<Boolean> getShouldForceExit();

    public abstract Property<Boolean> getIsClient();

    public abstract Property<Boolean> getShouldInheritMods();

    public NamedDomainObjectContainer<ModConfig> getMods() {
        return mods;
    }

    public abstract ListProperty<RunConfig> getParents();

    public abstract ListProperty<RunConfig> getChildren();

    public void merge(final RunConfig other, boolean overwrite) {

        RunConfig first = overwrite ? other : this;
        RunConfig second = overwrite ? this : other;

        this.runtimeProgramArguments = mergePropertiesIfInherited(this.getShouldInheritProgramArguments(), this.runtimeProgramArguments, other.getRuntimeProgramArguments(), RunConfig::mergeLists);
        this.runtimeJvmArguments = mergePropertiesIfInherited(this.getShouldInheritJvmArguments(), this.runtimeJvmArguments, other.getRuntimeJvmArguments(), RunConfig::mergeLists);

        getMainClass().set(first.getMainClass().orElse(second.getMainClass()));

        mergeOrOverrideProperties(getMainClass(), overwrite, other.getMainClass());
        mergeOrOverrideProperties(getShouldBuildAllProjects(), overwrite, other.getShouldBuildAllProjects());
        mergeOrOverrideProperties(getShouldForceExit(), overwrite, other.getShouldForceExit());
        mergeOrOverrideProperties(getShouldBeSingleInstanceOnly(), overwrite, other.getShouldBeSingleInstanceOnly());
        mergeOrOverrideProperties(getIdeaModuleName(), overwrite, other.getIdeaModuleName());
        mergeOrOverrideProperties(getWorkingDirectory(), overwrite, other.getWorkingDirectory());
        mergeOrOverrideProperties(getIsClient(), overwrite, other.getIsClient());

        this.runtimeEnvironmentVariables = mergePropertiesIfInherited(this.getShouldInheritEnvironmentVariables(), this.runtimeEnvironmentVariables, other.getRuntimeEnvironmentVariables(), RunConfig::mergeMaps);
        this.runtimeSystemProperties = mergePropertiesIfInherited(this.getShouldInheritSystemProperties(), this.runtimeSystemProperties, other.getRuntimeSystemProperties(), RunConfig::mergeMaps);



        if (other.mods != null) {
            other.mods.forEach(otherMod -> {
                final ModConfig thisMod = getMods().findByName(otherMod.getName());

                if (thisMod == null) {
                    getMods().add(otherMod);
                } else {
                    thisMod.merge(otherMod, false);
                }
            });
        }
    }

    private <V> BiFunction<NamedDomainObjectContainer<V>, NamedDomainObjectContainer<V>, NamedDomainObjectContainer<V>> buildContainerMerger(final Class<V> vClass) {
        return (first, second) -> mergeContainers(project, vClass, first, second);
    }

    private static <T> List<T> mergeLists(final List<T> left, final List<T> right) {
        final List<T> newList = new ArrayList<>(left);
        newList.addAll(right);
        return newList;
    }

    private static <K, V> Map<K, V> mergeMaps(final Map<K, V> left, final Map<K, V> right) {
        final Map<K, V> newList = new HashMap<>(left);
        newList.putAll(right);
        return newList;
    }

    private static <V> NamedDomainObjectContainer<V> mergeContainers(final Project project, Class<V> vClass, final NamedDomainObjectContainer<V> left, final NamedDomainObjectContainer<V> right) {
        final NamedDomainObjectContainer<V> newList = project.container(vClass);
        newList.addAll(left);
        newList.addAll(right);
        return newList;
    }

    private static <V> Provider<V> mergePropertiesIfInherited(Property<Boolean> shouldInheritProperty, Provider<V> current, Provider<V> other, BiFunction<V, V, V> transformer) {
        return shouldInheritProperty.flatMap(
                shouldInherit -> {
                    if (shouldInherit) {
                        return current.map(actualArguments -> transformer.apply(actualArguments, other.get()));
                    }

                    return current;
                }
        );
    }

    private static <T> void mergeOrOverrideProperties(final Property<T> target, final boolean override, final Property<T> other) {
        if (override) {
            target.set(other);
            return;
        }

        target.convention(other);
    }

    public void merge(List<RunConfig> runs) {
        runs.stream().distinct().filter(run -> run != this).forEach(run -> merge(run, false));
    }

    public void mergeParents() {
        merge(getParents().get());
    }


    public void setTokens(Map<String, String> tokens) {
        this.tokens = new HashMap<>(tokens);
    }

    public void token(String key, String value) {
        getTokens().put(key, value);
    }

    public void tokens(Map<String, String> tokens) {
        getTokens().putAll(tokens);
    }

    public Map<String, String> getTokens() {
        if (tokens == null) {
            tokens = new HashMap<>();
        }

        return tokens;
    }

    public void setLazyTokens(Map<String, Supplier<String>> lazyTokens) {
        this.lazyTokens = new HashMap<>(lazyTokens);
    }

    public void lazyToken(String key, Supplier<String> valueSupplier) {
        getLazyTokens().put(key, valueSupplier);
    }

    public void lazyToken(String key, Closure<String> closure) {
        lazyToken(key, closure::call);
    }

    public void lazyTokens(Map<String, Supplier<String>> lazyTokens) {
        getLazyTokens().putAll(lazyTokens);
    }

    public Map<String, Supplier<String>> getLazyTokens() {
        if (lazyTokens == null) {
            lazyTokens = new HashMap<>();
        }

        return lazyTokens;
    }

    public String replace(Map<String, ?> vars, String value) {
        if (value.length() <= 2 || value.indexOf('{') == -1) {
            return value;
        }

        return Utils.replaceTokens(vars, value);
    }

    public void client(boolean value) {
        setClient(value);
    }

    public void setClient(boolean value) {
        this.client = value;
    }

    public boolean isClient() {
        if (client == null) {
            boolean isTargetClient = getEnvironmentVariables().getOrDefault("target", "").contains("client") || getName().contains("client");

            client = isTargetClient || MCP_CLIENT_MAIN.equals(getMain()) || MC_CLIENT_MAIN.equals(getMain());
        }

        return client;
    }

    public List<SourceSet> getAllSources() {
        List<SourceSet> sources = getSources();

        getMods().stream().map(ModConfig::getSources).flatMap(Collection::stream).forEach(sources::add);

        sources = sources.stream().distinct().collect(Collectors.toList());

        if (sources.isEmpty()) {
            final JavaPluginExtension main = project.getExtensions().getByType(JavaPluginExtension.class);

            sources.add(main.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME));
        }

        return sources;
    }

    @Override
    public String toString() {
        return "RunConfig[project='" + project.getPath() + "', name='" + getName() + "']";
    }

}
