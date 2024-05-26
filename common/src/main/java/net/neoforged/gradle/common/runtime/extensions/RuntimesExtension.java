package net.neoforged.gradle.common.runtime.extensions;

import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import org.gradle.api.artifacts.Dependency;

import java.util.*;

public abstract class RuntimesExtension {
    private final List<CommonRuntimeExtension<?,?,?>> runtimeExtensions = new ArrayList<>();

    public void add(CommonRuntimeExtension<?,?,?> runtimeExtension) {
        runtimeExtensions.add(runtimeExtension);
    }

    public Collection<? extends CommonRuntimeDefinition<?>> getAllDefinitions() {
        List<CommonRuntimeDefinition<?>> definitions = new ArrayList<>();
        for (CommonRuntimeExtension<?,?,?> runtimeExtension : runtimeExtensions) {
            definitions.addAll(runtimeExtension.definitions.values());
        }
        return definitions;
    }

    public Map<String, Dependency> getAllDependencies() {
        Map<String, Dependency> dependencies = new HashMap<>();
        for (CommonRuntimeExtension<?,?,?> runtimeExtension : runtimeExtensions) {
            dependencies.putAll(runtimeExtension.dependencies);
        }
        return dependencies;
    }

    public boolean definitionExists(String identifier) {
        for (CommonRuntimeExtension<?,?,?> runtimeExtension : runtimeExtensions) {
            if (runtimeExtension.definitions.containsKey(identifier)) {
                return true;
            }
        }
        return false;
    }

    public <T extends CommonRuntimeDefinition<?>> T findDefinitionByNameOrIdentifier(String name) {
        for (CommonRuntimeExtension<?,?,?> runtimeExtension : runtimeExtensions) {
            CommonRuntimeDefinition<?> definition = runtimeExtension.findByNameOrIdentifier(name);
            if (definition != null) {
                return (T) definition;
            }
        }
        return null;
    }
}
