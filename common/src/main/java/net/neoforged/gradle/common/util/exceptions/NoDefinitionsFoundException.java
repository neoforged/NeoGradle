package net.neoforged.gradle.common.util.exceptions;

import net.neoforged.gradle.dsl.common.runtime.definition.Definition;

import java.util.List;
import java.util.stream.Collectors;

public class NoDefinitionsFoundException extends Exception {
    
    public NoDefinitionsFoundException() {
        super("Could not find the runtime definition to use.");
    }

    public NoDefinitionsFoundException(String message) {
        super(message);
    }
}