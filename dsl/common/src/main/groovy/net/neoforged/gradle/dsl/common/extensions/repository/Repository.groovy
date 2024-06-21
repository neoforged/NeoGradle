package net.neoforged.gradle.dsl.common.extensions.repository

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

/**
 * Defines a dummy repository extension which allows for the specification of dummy repository entries
 * which can then be used to dynamically generate compileDependencies for the project.
 */
@CompileStatic
interface Repository extends BaseDSLElement<Repository> {

    /**
     * Defines the directory which functions as root for the dummy repository.
     *
     * @return The directory where the dummy repository should be generated.
     */
    @DSLProperty
    DirectoryProperty getRepositoryDirectory();

    /**
     * Adds a new dependency to the dummy repository.
     *
     * @param entryBuilder The builder for creating entries.
     * @return The entry which was created.
     */
    Entry withEntry(EntryDefinition entryBuilder)

    /**
     * Calculates the path to the given entry.
     *
     * @param entry The entry to calculate the path for.
     * @param variant The variant of the entry to calculate the path for.
     * @return The path to the entry.
     */
    RegularFileProperty createOutputFor(Entry entry, Variant variant)

    /**
     * Calculates the path to the given dependency.
     *
     * @param dependency The dependency to calculate the path for.
     * @param variant The variant of the dependency to calculate the path for.
     * @return The path to the dependency.
     */
    RegularFileProperty createOutputFor(Dependency dependency, Variant variant)

    /**
     * Indicates whether the given dependency is already a dynamically generated dependency (whether already populated by task or not)
     *
     * @param dependency The dependency to check.
     * @return True when the dependency should be considered dynamic, false when not.
     */
    boolean isDynamicDependency(ModuleDependency dependency)

    /**
     * @returns the entries that are currently in the repository.
     */
    Set<Entry> getEntries()

    /**
     * Enables the repository.
     */
    void enable()

    public static enum Variant {
        RETAINED_CLASSIFIER(""),
        SOURCES_CLASSIFIER("sources"),
        ;

        private final String classifier;

        Variant(String classifier) {
            this.classifier = classifier
        }

        String adaptClassifier(String classifier) {
            if (this.classifier.isEmpty()) {
                return classifier
            } else {
                return this.classifier
            }
        }
    }
}
