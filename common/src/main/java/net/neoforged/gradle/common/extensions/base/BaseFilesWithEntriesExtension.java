package net.neoforged.gradle.common.extensions.base;

import net.minecraftforge.gdi.BaseDSLElementWithFilesAndEntries;
import org.gradle.api.Project;

import javax.inject.Inject;
import java.util.Collections;

/**
 * Represents part of an extension which combines a set of files with entries as well as raw addable entries.
 */
public abstract class BaseFilesWithEntriesExtension<TSelf extends BaseDSLElementWithFilesAndEntries<TSelf, TEntry>, TEntry> implements BaseDSLElementWithFilesAndEntries<TSelf, TEntry> {

    private final Project project;

    @Inject
    public BaseFilesWithEntriesExtension(Project project) {
        this.project = project;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public boolean isEmpty() {
        return getFiles().isEmpty() && getEntries().getOrElse(Collections.emptyList()).isEmpty();
    }
}
