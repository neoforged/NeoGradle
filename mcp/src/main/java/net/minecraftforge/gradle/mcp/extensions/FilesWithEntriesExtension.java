package net.minecraftforge.gradle.mcp.extensions;

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.inject.Inject;
import java.util.Collections;

/**
 * Represents part of an extension which combines a set of files with entries as well as raw addable entries.
 */
public abstract class FilesWithEntriesExtension implements Configurable<FilesWithEntriesExtension> {

    private final Project project;

    @Inject
    public FilesWithEntriesExtension(Project project) {
        this.project = project;
    }

    /**
     * @return The project this extension belongs to.
     */
    public Project getProject() {
        return project;
    }

    /**
     * @return The files which contain entries relevant to this extension.
     */
    public abstract ConfigurableFileCollection getFiles();

    /**
     * @return The raw additional entries relevant to this extension.
     */
    public abstract ListProperty<String> getEntries();

    /**
     * Configures this extension via the given closure.
     * @param closure The closure to configure this object with.
     * @return This object.
     */
    @SuppressWarnings("deprecation") //For now this is usable, if it ever gets removed, which is doubtfully, switch to gradle's own internal configure util.
    @Override
    public FilesWithEntriesExtension configure(Closure closure) {
        return ConfigureUtil.configureSelf(closure, this);
    }

    /**
     * Indicates if either at least one file is specified or at least one additional raw entry is specified.
     * @return {@code true}, when at least one file or entry is specified. False otherwise.
     */
    public boolean isEmpty() {
        return getFiles().isEmpty() && getEntries().getOrElse(Collections.emptyList()).isEmpty();
    }
}
