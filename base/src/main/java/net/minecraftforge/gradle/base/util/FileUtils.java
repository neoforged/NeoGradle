package net.minecraftforge.gradle.base.util;

import com.google.common.collect.Sets;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class FileUtils {

    private static final int MAX_TRIES = 2;
    private static final long ZIPTIME = 628041600000L;
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private FileUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: FileUtils. This is a utility class");
    }

    public static byte[] readAllBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read all bytes!", e);
        }
    }

    public static Stream<String> readAllLines(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read all lines!", e);
        }
    }

    public static void unzip(File zipFile, File dir) {
        // create output directory if it doesn't exist
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(dir.getAbsolutePath() + File.separator + fileName);
                if (ze.isDirectory())
                {
                    newFile.mkdirs();
                }
                else
                {
                    new File(newFile.getParent()).mkdirs();
                    newFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    zis.closeEntry();
                }

                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @NotNull
    public static String buildFileNameForTask(Project project, String taskName) {
        if (project.getPath().length() > 1) {
            return project.getPath().substring(1).replace(':', '_') + '_' + taskName;
        }

        return taskName;
    }

    public static Path temporaryPath(Path parent, String key) throws IOException {
        return Files.createTempFile(parent, "." + key, "");
    }

    @SuppressWarnings("BusyWait")
    public static void atomicMove(Path source, Path destination) throws IOException {
        try {
            FileUtils.atomicMoveIfPossible(source, destination);
        } catch (final AccessDeniedException ex) {
            // Sometimes because of file locking this will fail... Let's just try again and hope for the best
            // Thanks Windows!
            for (int tries = 0; true; ++tries) {
                // Pause for a bit
                try {
                    Thread.sleep(10L * tries);
                    FileUtils.atomicMoveIfPossible(source, destination);
                    return;
                } catch (final AccessDeniedException ex2) {
                    if (tries == FileUtils.MAX_TRIES - 1) {
                        throw ex;
                    }
                } catch (final InterruptedException exInterrupt) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
    }

    private static void atomicMoveIfPossible(final Path source, final Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AtomicMoveNotSupportedException ex) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static int getFileSize(File asFile) {
        return readAllBytes(asFile.toPath()).length;
    }

    public static void extractZip(final ArchiveOperations archiveOperations, final File input, final File output, final boolean overrideExisting, final boolean clean, final Action<? super PatternFilterable> filter, final Function<String, String> renamer, final GradleInternalUtils.ProgressLoggerWrapper loggerWrapper) throws IOException {
        final FileTree tree = archiveOperations.zipTree(input);
        final FileTree filtered = tree.matching(filter);

        final Set<File> extracted = Sets.newHashSet();
        final Set<File> current = Sets.newHashSet();
        final Stream<Path> stream = Files.walk(output.toPath());
        stream.forEach(path -> current.add(path.toFile()));
        stream.close();

        loggerWrapper.setSize(filtered.getFiles().size());
        loggerWrapper.started();

        filtered.visit(new FileVisitor() {
            @Override
            public void visitDir(FileVisitDetails dirDetails) {
                final File dir = new File(output, dirDetails.getPath());
                dir.mkdirs();
            }

            @Override
            public void visitFile(FileVisitDetails fileDetails) {
                loggerWrapper.incrementProcessedFileCount();
                final File outputFile = new File(output, renamer.apply(fileDetails.getPath()));
                extracted.add(outputFile);

                if (!overrideExisting && outputFile.exists()) {
                    return;
                }

                outputFile.getParentFile().mkdirs();
                try {
                    Files.copy(fileDetails.getFile().toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy file: " + fileDetails.getPath(), e);
                }
            }
        });

        if (clean) {
            final Set<File> toDelete = current.stream().filter(file -> !extracted.contains(file)).collect(Collectors.toSet());
            toDelete.forEach(File::delete);
        }

        loggerWrapper.completed();
    }

    public static void addCsvToZip(String name, List<String[]> mappings, ZipOutputStream out) throws IOException {
        if (mappings.size() <= 1)
            return;
        out.putNextEntry(getStableEntry(name));
        try (CsvWriter writer = CsvWriter.builder().lineDelimiter(LineDelimiter.LF).build(new UncloseableOutputStreamWriter(out))) {
            mappings.forEach(writer::writeRow);
        }
        out.closeEntry();
    }

    private static class UncloseableOutputStreamWriter extends OutputStreamWriter {
        private UncloseableOutputStreamWriter(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            super.flush();
        }
    }

    public static ZipEntry getStableEntry(String name) {
        return getStableEntry(name, ZIPTIME);
    }

    public static ZipEntry getStableEntry(String name, long time) {
        TimeZone _default = TimeZone.getDefault();
        TimeZone.setDefault(GMT);
        ZipEntry ret = new ZipEntry(name);
        ret.setTime(time);
        TimeZone.setDefault(_default);
        return ret;
    }
}
