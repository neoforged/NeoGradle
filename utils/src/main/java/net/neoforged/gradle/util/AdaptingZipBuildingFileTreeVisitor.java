package net.neoforged.gradle.util;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.tasks.OutputFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A {@link ZipBuildingFileTreeVisitor} that allows to adapt the file content before writing it to the zip.
 */
public class AdaptingZipBuildingFileTreeVisitor extends ZipBuildingFileTreeVisitor {

    private final Adapter fileAdapter;

    public AdaptingZipBuildingFileTreeVisitor(ZipOutputStream outputZipStream, Adapter fileAdapter) {
        super(outputZipStream);
        this.fileAdapter = fileAdapter;
    }

    @Override
    public void visitFile(FileVisitDetails fileVisitDetails) {
        try {
            final ZipEntry fileEntry = new ZipEntry(fileVisitDetails.getRelativePath().getPathString());
            outputZipStream.putNextEntry(fileEntry);
            fileAdapter.accept(fileVisitDetails, outputZipStream);
            outputZipStream.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Could not create zip file: " + fileVisitDetails.getRelativePath().getPathString(), e);
        }
    }

    public interface Adapter {
        void accept(FileVisitDetails details, OutputStream outputStream) throws IOException;
    }
}
