package net.minecraftforge.gradle.common.util.exceptions;

import net.minecraftforge.gradle.common.runtime.CommonRuntimeDefinition;

import java.util.List;
import java.util.stream.Collectors;

public class MultipleDefinitionsFoundException extends Exception {

    private final List<? extends CommonRuntimeDefinition<?>> definitions;

    public MultipleDefinitionsFoundException(List<? extends CommonRuntimeDefinition<?>> definitions) {
        super("Could not determine the runtime definition to use. Multiple definitions were found: " + definitions.stream().map(r -> r.spec().getName()).collect(Collectors.joining(", ")));
        this.definitions = definitions;
    }

    public List<? extends CommonRuntimeDefinition<?>> getDefinitions() {
        return definitions;
    }
}
