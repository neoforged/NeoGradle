package net.minecraftforge.gradle.dsl.common.extensions.repository

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.annotations.DSLProperty
import net.minecraftforge.gradle.dsl.base.BaseDSLElement
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

import javax.xml.stream.XMLStreamException
import java.util.function.Consumer

/**
 * Defines a dummy repository extension which allows for the specification of dummy repository entries
 * which can then be used to dynamically generate dependencies for the project.
 *
 * @param <TEntry> The type of the entry which is used to define the dummy repository entries.
 * @param <TEntryBuilder> The type of the entry builder which is used to define the dummy repository entries.
 * @param <TDependency> The type of the dependency which is used to define the dummy repository entry dependencies.
 * @param <TDependencyBuilder> The type of the dependency builder which is used to define the dummy repository entry dependencies.
 */
@CompileStatic
interface Repository<TSelf extends Repository<TSelf, TEntry, TEntryBuilder, TDependency, TDependencyBuilder>, TEntry extends RepositoryEntry<TEntry, TDependency>, TEntryBuilder extends RepositoryEntry.Builder<TEntryBuilder, TDependency, TDependencyBuilder>, TDependency extends RepositoryReference, TDependencyBuilder extends RepositoryReference.Builder<TDependencyBuilder, TDependency>> extends BaseDSLElement<TSelf> {

    /**
     * Defines the directory which functions as root for the dummy repository.
     *
     * @return The directory where the dummy repository should be generated.
     */
    @DSLProperty
    DirectoryProperty getRepositoryDirectory();

    /**
     * Adds a new dependency to the dummy repository.
     * The configurator is invoked immediately, but the entry is only generated when the dummy repository is generated during
     * the afterEvaluate phase of the owning project.
     *
     * @param configurator The configurator for the dependency.
     * @param configuredEntryConsumer The callback, called from an after evaluate phase, which receives the configured entry.
     * @throws XMLStreamException when the entry could not be generated because of violations in the XML structure.
     * @throws IOException when the entry could not be generated because of violations in the file system.
     */
    void withDependency(Action<TEntryBuilder> configurator, Action<TEntry> configuredEntryConsumer) throws XMLStreamException, IOException;

    /**
     * Allows for the registration of a callback that gets trigger in an after evaluate phase when the dummy repository is generated.
     *
     * @param projectConsumer The callback, called from an after evaluate phase, which receives the owning project.
     */
    void afterEntryRealisation(Consumer<Project> projectConsumer)
}
