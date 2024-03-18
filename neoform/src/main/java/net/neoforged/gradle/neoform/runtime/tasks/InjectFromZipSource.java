package net.neoforged.gradle.neoform.runtime.tasks;

import org.apache.commons.io.IOUtils;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Inject content from another ZIP-file.
 * @see InjectZipContent
 */
public abstract class InjectFromZipSource extends AbstractInjectSource {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getZipFile();

    @Override
    public byte @Nullable [] tryReadFile(String path) throws IOException {
        try (ZipFile zf = new ZipFile(getZipFile().getAsFile().get())) {
            ZipEntry entry = zf.getEntry(path);
            if (entry != null) {
                try (InputStream in = zf.getInputStream(entry)) {
                    return IOUtils.toByteArray(in);
                }
            }
        }
        return null;
    }

    @Override
    public void copyTo(ZipOutputStream out) throws IOException {
        Spec<FileTreeElement> spec = createFilter().getAsSpec();
        try (ZipInputStream zin = new ZipInputStream(new FileInputStream(getZipFile().getAsFile().get()))) {
            for (ZipEntry entry = zin.getNextEntry(); entry != null; entry = zin.getNextEntry()) {
                // Apply filter with a "fake" file
                if (spec.isSatisfiedBy(new ZipEntryFileTreeElement(entry))) {
                    try {
                        out.putNextEntry(entry);
                        IOUtils.copyLarge(zin, out);
                        out.closeEntry();
                    } catch (ZipException e) {
                        if (!e.getMessage().startsWith("duplicate entry:")) {
                            throw e;
                        } else if (!entry.isDirectory()) {
                            // Warn on duplicate files, but ignore duplicate directories
                            getLogger().warn("Cannot inject duplicate file {}", entry.getName());
                        }
                    }
                }
            }
        }
    }
}
