package net.minecraftforge.gradle.mcp.util;

import org.gradle.api.file.FileVisitDetails;

import java.util.function.Predicate;
import java.util.zip.ZipOutputStream;

public final class FilteringZipBuildingFileTreeVisitor extends ZipBuildingFileTreeVisitor {

    private final Predicate<String> filter;

    public FilteringZipBuildingFileTreeVisitor(ZipOutputStream outputZipStream, Predicate<String> filter) {
        super(outputZipStream);
        this.filter = filter;
    }

    @Override
    public void visitDir(FileVisitDetails fileVisitDetails) {
        if (filter.test(fileVisitDetails.getRelativePath().getPathString())) {
            super.visitDir(fileVisitDetails);
        }
    }

    @Override
    public void visitFile(FileVisitDetails fileVisitDetails) {
        if (filter.test(fileVisitDetails.getRelativePath().getPathString())) {
            super.visitFile(fileVisitDetails);
        }
    }
}
