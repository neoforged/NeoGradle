package net.neoforged.gradle.common.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.EnumSet;
import java.util.Set;

public class ConfigurationPhaseFileUtils {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodType BOOL_RETURN_TYPE_NO_ARGS = MethodType.methodType(boolean.class);
    private static final MethodType FILE_ARRAY_RETURN_TYPE_FILE_FILTER_ARGS = MethodType.methodType(File[].class, FileFilter.class);
    private static final MethodType BOOL_RETURN_TYPE_PATH_LINK_OPTIONS_ARRAY_ARGS = MethodType.methodType(boolean.class, Path.class, LinkOption[].class);
    private static final MethodType SEEKABLE_BYTE_CHANNEL_RETURN_TYPE_PATH_SET_OPEN_OPTION_FILE_ATTRIBUTE_ARRAY_ARGS = MethodType.methodType(SeekableByteChannel.class, Path.class, Set.class, FileAttribute[].class);

    public static boolean exists(File file) {
        try {
            return (boolean) LOOKUP.findVirtual(File.class, "exists", BOOL_RETURN_TYPE_NO_ARGS).invoke(file);
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean mkdirs(File file) {
        try {
            return (boolean) LOOKUP.findVirtual(File.class, "mkdirs", BOOL_RETURN_TYPE_NO_ARGS).invoke(file);
        } catch (Throwable e) {
            return false;
        }
    }

    public static File[] listFiles(File file, FileFilter filter) {
        try {
            return (File[]) LOOKUP.findVirtual(File.class, "listFiles", FILE_ARRAY_RETURN_TYPE_FILE_FILTER_ARGS).invoke(file, filter);
        } catch (Throwable e) {
            return new File[0];
        }
    }

    public static boolean isRegularFile(Path path, LinkOption... options) {
        try {
            return (boolean) LOOKUP.findStatic(Files.class, "isRegularFile", BOOL_RETURN_TYPE_PATH_LINK_OPTIONS_ARRAY_ARGS).invoke(path, options);
        } catch (Throwable e) {
            return false;
        }
    }

    public static Path createFile(Path path) throws IOException {
        EnumSet<StandardOpenOption> options =
                EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        newByteChannel(path, options).close();
        return path;
    }

    public static SeekableByteChannel newByteChannel(Path path,
                                                     Set<? extends OpenOption> options,
                                                     FileAttribute<?>... attrs) {
        try {
            return (SeekableByteChannel) LOOKUP.findStatic(Files.class, "newByteChannel", SEEKABLE_BYTE_CHANNEL_RETURN_TYPE_PATH_SET_OPEN_OPTION_FILE_ATTRIBUTE_ARRAY_ARGS).invoke(path, options, attrs);
        } catch (Throwable e) {
            return null;
        }
    }
}
