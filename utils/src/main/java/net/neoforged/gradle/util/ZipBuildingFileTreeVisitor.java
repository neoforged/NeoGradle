package net.neoforged.gradle.util;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * A {@link FileVisitor} that builds a zip file
 * Simply copying all visited files to the given {@link ZipOutputStream} as individual entries
 */
public class ZipBuildingFileTreeVisitor implements FileVisitor {

    protected final ZipOutputStream outputZipStream;
    
    public ZipBuildingFileTreeVisitor(ZipOutputStream outputZipStream) {
        this.outputZipStream = outputZipStream;
    }

    @Override
    public void visitDir(FileVisitDetails fileVisitDetails) {
        try {
            final ZipEntry directoryEntry = createDirectory(fileVisitDetails);
            outputZipStream.putNextEntry(directoryEntry);
            outputZipStream.closeEntry();
        } catch (IOException zip) {
            if (!zip.getMessage().equals("duplicate entry: " + fileVisitDetails.getRelativePath().getPathString() + "/")) {
                throw new RuntimeException("Could not create zip directory: " + fileVisitDetails.getRelativePath().getPathString(), zip);
            }
        }
    }
    
    protected ZipEntry createDirectory(FileVisitDetails fileVisitDetails) {
        return new ZipEntry(fileVisitDetails.getRelativePath().getPathString() + "/");
    }

    @Override
    public void visitFile(FileVisitDetails fileVisitDetails) {
        try {
            final ZipEntry fileEntry = createFile(fileVisitDetails);
            outputZipStream.putNextEntry(fileEntry);
            fileVisitDetails.copyTo(outputZipStream);
            outputZipStream.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException("Could not create zip file: " + fileVisitDetails.getRelativePath().getPathString(), e);
        }
    }

    protected ZipEntry createFile(FileVisitDetails fileVisitDetails) {
        return new ZipEntry(fileVisitDetails.getRelativePath().getPathString());
    }
}
