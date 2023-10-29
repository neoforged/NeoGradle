package net.neoforged.gradle.common.runtime.extensions;

import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class RuntimesExtension {
    private final List<CommonRuntimeExtension<?,?,?>> runtimeExtensions = new ArrayList<>();

    public void add(CommonRuntimeExtension<?,?,?> runtimeExtension) {
        runtimeExtensions.add(runtimeExtension);
    }

    public void bakeDefinitions() {
        for (CommonRuntimeExtension<?,?,?> runtimeExtension : runtimeExtensions) {
            runtimeExtension.bakeDefinitions();
        }
    }

    public void bakeDelegateDefinitions() {
        for (CommonRuntimeExtension<?,?,?> runtimeExtension : runtimeExtensions) {
            runtimeExtension.bakeDelegateDefinitions();
        }
    }

    public Collection<? extends CommonRuntimeDefinition<?>> getAllDefinitions() {
        List<CommonRuntimeDefinition<?>> definitions = new ArrayList<>();
        for (CommonRuntimeExtension<?,?,?> runtimeExtension : runtimeExtensions) {
            definitions.addAll(runtimeExtension.runtimes.values());
        }
        return definitions;
    }

    public boolean definitionExists(String identifier) {
        for (CommonRuntimeExtension<?,?,?> runtimeExtension : runtimeExtensions) {
            if (runtimeExtension.runtimes.containsKey(identifier)) {
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
