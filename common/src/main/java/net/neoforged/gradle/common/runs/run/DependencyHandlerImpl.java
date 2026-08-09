package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.dsl.common.runs.run.DependencyHandler;
import net.neoforged.gradle.common.util.ConfigurationUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ProjectDependency;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public abstract class DependencyHandlerImpl implements DependencyHandler {

    private final Project project;
    private final String context;
    private Configuration modSourceConfiguration;
    private final List<ModSourceDependency> modSourceDependencies = new ArrayList<>();

    @Inject
    public DependencyHandlerImpl(Project project, String context) {
        this.project = project;
        this.context = context;
    }

    public Project getProject() {
        return project;
    }

    public Configuration getRuntimeConfiguration() {
        final Configuration configuration = ConfigurationUtils.temporaryConfiguration(project, context);
        if (configuration.getDependencies().isEmpty()) {
            configuration.fromDependencyCollector(this.getRuntime());
        }
        return configuration;
    }

    // Delegate standard dependency methods to Gradle's dependency handler for proper notation support.
    // These are not part of the Dependencies interface but are needed for DSL compatibility (e.g., project() calls).

    @SuppressWarnings("unchecked")
    public ProjectDependency project(Object dependencyNotation) {
        Dependency dep;
        if (dependencyNotation instanceof java.util.Map<?, ?> map) {
            dep = project.getDependencies().project((java.util.Map<String, Object>) map);
        } else if (dependencyNotation instanceof String path) {
            dep = project.getDependencies().create(path);
        } else {
            // Fallback: try create() which handles various notations.
            dep = project.getDependencies().create(dependencyNotation);
        }
        if (!(dep instanceof ProjectDependency)) {
            throw new IllegalArgumentException("Expected a project dependency but got: " + dep.getClass().getName());
        }
        return (ProjectDependency) dep;
    }

    @SuppressWarnings("unchecked")
    public ProjectDependency project(Object dependencyNotation, groovy.lang.Closure<?> closure) {
        // For Map+Closure notation like project(path: ':api') { configuration = '...' },
        // we need to first create the base ProjectDependency from the map, then apply the closure.
        Dependency dep;
        if (dependencyNotation instanceof java.util.Map<?, ?> map) {
            dep = project.getDependencies().project((java.util.Map<String, Object>) map);
        } else if (dependencyNotation instanceof String path) {
            dep = project.getDependencies().create(path);
        } else {
            throw new IllegalArgumentException("Unsupported notation for project() with closure: " + dependencyNotation);
        }
        if (!(dep instanceof ProjectDependency)) {
            throw new IllegalArgumentException("Expected a project dependency but got: " + dep.getClass().getName());
        }
        ProjectDependency projDep = (ProjectDependency) dep;
        closure.setDelegate(projDep);
        closure.setResolveStrategy(groovy.lang.Closure.DELEGATE_FIRST);
        closure.call();
        return projDep;
    }

    @Override
    public void modSource(Object dependencyNotation) {
        modSource(null, dependencyNotation);
    }

    @Override
    public void modSource(String groupId, Object dependencyNotation) {
        Dependency dep = project.getDependencies().create(dependencyNotation);
        if (!(dep instanceof ProjectDependency)) {
            throw new IllegalArgumentException("modSource dependencies must be project dependencies. " +
                "Use: modSource project(path: ':api', configuration: 'main')");
        }
        ProjectDependency projDep = (ProjectDependency) dep;

        // Extract the target configuration name from the dependency notation.
        String configName = extractConfigurationName(dependencyNotation, projDep);
        if (configName == null || configName.isEmpty()) {
            throw new IllegalArgumentException("modSource dependencies must specify a configuration. " +
                "Use: modSource project(path: ':api', configuration: 'main')");
        }

        // Normalize the configuration name by applying the modSource prefix silently.
        String normalizedConfigName = normalizeConfigurationName(configName);

        // Recreate the dependency with the normalized configuration name so Gradle validates it properly.
        ProjectDependency normalizedDep = recreateWithNormalizedConfig(dependencyNotation, projDep, normalizedConfigName);

        modSourceDependencies.add(new ModSourceDependency(normalizedDep, groupId, normalizedConfigName));

        // Add to the mod source configuration so Gradle validates and resolves it
        getModSourceConfiguration().getDependencies().add(normalizedDep);
    }

    /**
     * Extracts the target configuration name from dependency notation.
     */
    private String extractConfigurationName(Object notation, ProjectDependency projDep) {
        // Handle Map-based notation: [path: ':api', configuration: 'main']
        if (notation instanceof java.util.Map<?, ?> mapNotation) {
            Object config = mapNotation.get("configuration");
            return config != null ? String.valueOf(config) : null;
        }

        // Handle Closure-based notation via the resolved ProjectDependency's target configuration.
        // Gradle stores this in the dependency's "targetConfiguration" property when using closure syntax.
        try {
            java.lang.reflect.Method method = projDep.getClass().getMethod("getTargetConfiguration");
            Object result = method.invoke(projDep);
            return result != null ? String.valueOf(result) : null;
        } catch (ReflectiveOperationException e) {
            // Fallback: try to get it from the dependency's toString or other means.
            // If this fails, Gradle will fail during resolution with a clear error anyway.
            return null;
        }
    }

    /**
     * Normalizes configuration names by applying the modSource prefix silently when appropriate.
     * <p>
     * This allows users to write {@code configuration: 'main'} instead of {@code configuration: 'modSourceMain'},
     * while still supporting explicit modSource* configurations for backward compatibility.
     * </p>
     */
    private String normalizeConfigurationName(String configName) {
        // If already starts with "modSource", use as-is (backward compatible).
        if (configName.startsWith("modSource")) {
            return configName;
        }

        // Apply the modSource prefix silently.
        // Gradle will validate at resolution time whether this configuration exists on the target project.
        return "modSource" + capitalize(configName);
    }

    /**
     * Recreates a ProjectDependency with a normalized configuration name.
     */
    @SuppressWarnings("unchecked")
    private ProjectDependency recreateWithNormalizedConfig(Object originalNotation, ProjectDependency originalDep, String normalizedConfigName) {
        // Handle Map-based notation: rebuild with the new config name.
        if (originalNotation instanceof java.util.Map<?, ?> mapNotation) {
            java.util.Map<String, Object> newMap = new java.util.HashMap<>();
            for (java.util.Map.Entry<?, ?> entry : mapNotation.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if ("configuration".equals(key)) {
                    newMap.put(key, normalizedConfigName);
                } else {
                    newMap.put(key, entry.getValue());
                }
            }
            return (ProjectDependency) project.getDependencies().project(newMap);
        }

        // For closure-based notation or other forms, fall back to the original dependency.
        // The configuration name is stored separately in ModSourceDependency for resolution time.
        return originalDep;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    @Override
    public Configuration getModSourceConfiguration() {
        if (modSourceConfiguration == null) {
            String configName = context + "ModSources";
            modSourceConfiguration = project.getConfigurations().create(configName, conf -> {
                conf.setVisible(false);
                conf.setCanBeConsumed(false);
                conf.setCanBeResolved(true);
            });
        }
        return modSourceConfiguration;
    }

    /**
     * Returns the list of declared mod source dependencies with their group IDs.
     */
    public List<ModSourceDependency> getModSourceDependencies() {
        return modSourceDependencies;
    }

    /**
     * Holds a mod source dependency along with its optional group ID override and target configuration name.
     */
    public static class ModSourceDependency {
        private final ProjectDependency projectDependency;
        private final String groupId; // null means use default from variant metadata or source set
        private final String configurationName;

        public ModSourceDependency(ProjectDependency projectDependency, String groupId, String configurationName) {
            this.projectDependency = projectDependency;
            this.groupId = groupId;
            this.configurationName = configurationName;
        }

        public ProjectDependency getProjectDependency() {
            return projectDependency;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getConfigurationName() {
            return configurationName;
        }
    }
}
