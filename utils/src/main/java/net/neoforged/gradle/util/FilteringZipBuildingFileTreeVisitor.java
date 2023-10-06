package net.neoforged.gradle.util;

import org.gradle.api.file.FileVisitDetails;

import java.util.function.Predicate;
import java.util.zip.ZipOutputStream;

/**
 * A {@link ZipBuildingFileTreeVisitor} that allows to adapt the file content before writing it to the zip.
 */
public class FilteringZipBuildingFileTreeVisitor extends ZipBuildingFileTreeVisitor {

    private final Predicate<FileVisitDetails> directoryFilter;

    private final Predicate<FileVisitDetails> fileFilter;

    public FilteringZipBuildingFileTreeVisitor(ZipOutputStream outputZipStream, Predicate<FileVisitDetails> directoryFilter, Predicate<FileVisitDetails> fileFilter) {
        super(outputZipStream);
        this.directoryFilter = directoryFilter;
        this.fileFilter = fileFilter;
    }
    
    @Override
    public void visitDir(FileVisitDetails fileVisitDetails) {
        if (!directoryFilter.test(fileVisitDetails)) {
            return;
        }
        
        super.visitDir(fileVisitDetails);
    }
    
    @Override
    public void visitFile(FileVisitDetails fileVisitDetails) {
        if (!fileFilter.test(fileVisitDetails)) {
            return;
        }
        
        super.visitFile(fileVisitDetails);
    }
}
