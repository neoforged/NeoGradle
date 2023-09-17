package net.neoforged.gradle.platform.extensions;

import org.gradle.api.UnknownProjectException;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines an extension for the {@link org.gradle.api.initialization.Settings} object which
 * can then be used to manage the dynamic projects contained in it.
 */
public abstract class DynamicProjectManagementExtension {

    private final Settings settings;
    private final Map<String, ProjectDescriptor> knownDynamicDescriptors = new HashMap<>();

    @Inject
    public DynamicProjectManagementExtension(Settings settings) {
        this.settings = settings;
    }

    public void include(String... projectPaths) {
        this.include(Arrays.asList(projectPaths));
    }

    public void include(Iterable<String> projectPaths) {
        settings.include(projectPaths);
        for (String projectPath : projectPaths) {
            knownDynamicDescriptors.put(projectPath, settings.project(projectPath));
        }
    }

    public ProjectDescriptor project(String path) throws UnknownProjectException {
        if (!knownDynamicDescriptors.containsKey(path))
            throw new UnknownProjectException(String.format("The given path: %s does not target a dynamic project", path));

        return knownDynamicDescriptors.get(path);
    }

    @Nullable
    public ProjectDescriptor findProject(String path) {
        return knownDynamicDescriptors.get(path);
    }

    public Collection<ProjectDescriptor> getDynamicProjects() {
        return knownDynamicDescriptors.values();
    }
}
