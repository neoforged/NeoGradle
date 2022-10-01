package net.minecraftforge.gradle.common.util;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class FileUtils {

    private static final int MAX_TRIES = 2;

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
}
