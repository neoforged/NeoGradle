package net.neoforged.gradle.dsl.common.runs.type

import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.NamedDomainObjectContainer

@CompileStatic
interface RunTypeManager extends NamedDomainObjectContainer<RunType> {

    /**
     * Parses the given file and returns a collection of run types.
     *
     * @param file The file to parse
     * @return The collection of run types
     */
    Collection<RunType> parse(File file);

    /**
     * Registers a parser for the given file extension.
     *
     * @param parser The parser
     */
    void registerParser(Parser parser);

    /**
     * Represents a parser of run types that can be loaded from a file.
     */
    static interface Parser {

        Collection<RunType> parse(File file);
    }
}
