package net.neoforged.gradle.util;

import com.google.common.collect.Sets;
import de.siegmar.fastcsv.writer.CsvWriter;
import de.siegmar.fastcsv.writer.LineDelimiter;
import org.gradle.api.Action;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.util.PatternFilterable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A utility class for file operations.
 */
public final class FileUtils {

    /**
     * The maximum number of tries that the system will try to atomically move a file.
     */
    private static final int MAX_TRIES = 2;

    /**
     * The constant time of a zip entry in milliseconds.
     */
    private static final long ZIPTIME = 628041600000L;

    /**
     * The GMT time zone.
     */
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private FileUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: FileUtils. This is a utility class");
    }

    /**
     * Reads all bytes from a file with the given path.
     * Catching all exceptions and rethrowing them as a {@link RuntimeException}.
     *
     * @param path the path to the file
     * @return the bytes of the file
     */
    public static byte[] readAllBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read all bytes!", e);
        }
    }

    /**
     * Reads all lines from a file with the given path.
     * Catching all exceptions and rethrowing them as a {@link RuntimeException}.
     *
     * @param path the path to the file
     * @return the lines of the file
     */
    public static Stream<String> readAllLines(Path path) {
        try {
            return Files.lines(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read all lines!", e);
        }
    }

    /**
     * Creates a path targeting a temporary file with the given key.
     *
     * @param parent The parent directory of the temporary file
     * @param key The key of the temporary file
     * @return The path to the temporary file
     * @throws IOException If an I/O error occurs
     */
    public static Path temporaryPath(Path parent, String key) throws IOException {
        return Files.createTempFile(parent, "." + key, "");
    }

    /**
     * Atomically moves the given source file to the given destination file.
     *
     * @param source The source file
     * @param destination The destination file
     * @throws IOException If an I/O error occurs
     */
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

    /**
     * Atomically moves the given source file to the given destination file.
     * If the atomic move is not supported, the file will be moved normally.
     *
     * @param source The source file
     * @param destination The destination file
     * @throws IOException If an I/O error occurs
     */
    private static void atomicMoveIfPossible(final Path source, final Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AtomicMoveNotSupportedException ex) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Gets the size in bytes of the file.
     *
     * @param asFile The file to get the size of
     * @return The size in bytes of the file
     */
    public static int getFileSize(File asFile) {
        return readAllBytes(asFile.toPath()).length;
    }

    /**
     * Performs a complex zip extraction from the input zip file to the output directory.
     *
     * @param archiveOperations The archive operations
     * @param input The input zip file
     * @param output The output directory
     * @param overrideExisting Whether to override existing files
     * @param clean Whether to clean the output directory
     * @param filter The filter to apply to the zip file
     * @param renamer The renamer to apply to the zip file
     * @param loggerWrapper The progress logger wrapper
     * @throws IOException If an I/O error occurs
     * @implNote This method does not use the {@link CopyingFileTreeVisitor} since it has some special functions.
     */
    //TODO: In the future expand this method to use the CopyingFileTreeVisitor instead of a custom implementation.
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

    /**
     * Creates a csv file with the given lines in a zip file via the given zip output stream.
     * If no lines are supplied, the csv file will not be created.
     *
     * @param name The name of the csv file
     * @param mappings The lines of the csv file
     * @param out The zip output stream
     * @throws IOException If an I/O error occurs
     */
    public static void addCsvToZip(String name, List<String[]> mappings, ZipOutputStream out) throws IOException {
        if (mappings.size() <= 1)
            return;
        out.putNextEntry(getStableEntry(name));
        try (CsvWriter writer = CsvWriter.builder().lineDelimiter(LineDelimiter.LF).build(new UncloseableOutputStreamWriter(out))) {
            mappings.forEach(writer::writeRow);
        }
        out.closeEntry();
    }

    /**
     * An output stream writer that does not close the underlying output stream when it is closed.
     */
    private static class UncloseableOutputStreamWriter extends OutputStreamWriter {
        private UncloseableOutputStreamWriter(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            super.flush();
        }
    }

    /**
     * Creates a stable timed zip entry, with the default time.
     *
     * @param name The relative name of the entry
     * @return The zip entry
     */
    public static ZipEntry getStableEntry(String name) {
        return getStableEntry(name, ZIPTIME);
    }

    /**
     * Creates a stable timed zip entry.
     *
     * @param name The relative name of the entry
     * @param time The time of the entry
     * @return The zip entry
     */
    public static ZipEntry getStableEntry(String name, long time) {
        TimeZone _default = TimeZone.getDefault();
        TimeZone.setDefault(GMT);
        ZipEntry ret = new ZipEntry(name);
        ret.setTime(time);
        TimeZone.setDefault(_default);
        return ret;
    }

    public static void delete(final File file) {
        if (file.isFile()) {
            file.delete();
        }

        if (file.isDirectory()) {
            for (File listFile : Objects.requireNonNull(file.listFiles())) {
                delete(listFile);
            }

            file.delete();
        }
    }
}
