package net.neoforged.gradle.common.util.exceptions;

public class NoDefinitionsFoundException extends Exception {
    
    public NoDefinitionsFoundException() {
        super("Could not find the runtime definition to use.");
    }
}