package net.neoforged.gradle.dsl.common.extensions.repository

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.DirectoryProperty

import javax.xml.stream.XMLStreamException
import java.util.function.Consumer

/**
 * Defines a dummy repository extension which allows for the specification of dummy repository entries
 * which can then be used to dynamically generate dependencies for the project.
 */
@CompileStatic
interface Repository<TSelf extends Repository<TSelf>> extends BaseDSLElement<TSelf> {

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
     * @param referenceBuilder The builder for creating references.
     * @param onReferenceBuild Callback triggered with a build reference which can be added as a dependency to any configuration.
     * @param configurator The configurator for the dependency.
     * @param configuredEntryConsumer The callback, called from an after evaluate phase, which receives the configured entry.
     * @param processImmediately Indicates whether the repository should immediately start with processing of the request, or delay it until after evaluation
     * @throws XMLStreamException when the entry could not be generated because of violations in the XML structure.
     * @throws IOException when the entry could not be generated because of violations in the file system.
     */
    void withDependency(Action<RepositoryReference.Builder<?,?>> referenceBuilder, Action<RepositoryReference> onReferenceBuild, Action<RepositoryEntry.Builder<?,?,?>> configurator, Action<RepositoryEntry<?,?>> configuredEntryConsumer, boolean processImmediately) throws XMLStreamException, IOException;

    /**
     * Allows for the registration of a callback that gets trigger in an after evaluate phase when the dummy repository is generated.
     *
     * @param projectConsumer The callback, called from an after evaluate phase, which receives the owning project.
     */
    void afterEntryRealisation(Consumer<Project> projectConsumer)

    /**
     * Indicates whether the given dependency is already a dynamically generated dependency (whether already populated by task or not)
     *
     * @param dependency The dependency to check.
     * @return True when the dependency should be considered dynamic, false when not.
     */
    boolean isDynamicDependency(ModuleDependency dependency)
}
