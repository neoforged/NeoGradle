package net.neoforged.gradle.common.util.exceptions;

import net.neoforged.gradle.dsl.common.runtime.definition.Definition;

import java.util.List;
import java.util.stream.Collectors;

public class MultipleDefinitionsFoundException extends Exception {

    private final List<? extends Definition<?>> definitions;

    public MultipleDefinitionsFoundException(List<? extends Definition<?>> definitions) {
        super("Could not determine the runtime definition to use. Multiple definitions were found: " + definitions.stream().map(r -> r.getSpecification().getVersionedName()).collect(Collectors.joining(", ")));
        this.definitions = definitions;
    }

    public List<? extends Definition<?>> getDefinitions() {
        return definitions;
    }
}
