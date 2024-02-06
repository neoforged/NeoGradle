package net.neoforged.gradle.neoform.runtime.tasks;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.util.PatternSet;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

/**
 * Defines a source for injecting content into a zip file using the {@link InjectZipContent} task.
 * @see InjectFromDirectorySource
 * @see InjectFromZipSource
 */
public abstract class AbstractInjectSource {
    /**
     * Optional Ant-Style include filters that limit what is included from the source.
     * @see org.gradle.api.tasks.util.PatternFilterable
     */
    @Input
    @Optional
    public abstract ListProperty<String> getInclusionFilter();

    /**
     * Optional Ant-Style exclude filters that limit what is included from the source.
     * @see org.gradle.api.tasks.util.PatternFilterable
     */
    @Input
    @Optional
    public abstract ListProperty<String> getExclusionFilter();

    /**
     * Tries to read a file from this source.
     *
     * @return null if the file doesn't exist.
     */
    public abstract byte @Nullable [] tryReadFile(String path) throws IOException;

    /**
     * Copy the contents of this source to the given zip output stream, while applying the filters
     * defined in {@link #getInclusionFilter()} and {@link #getExclusionFilter()}.
     * <p>
     * Files that have already been written to {@code out} should issue a warning, while directories
     * should simply be ignored.
     */
    public abstract void copyTo(ZipOutputStream out) throws IOException;

    @Inject
    protected abstract Project getProject();

    protected final PatternSet createFilter() {
        PatternSet filter = new PatternSet();
        filter.include(getInclusionFilter().get());
        filter.exclude(getExclusionFilter().get());
        return filter;
    }
}

