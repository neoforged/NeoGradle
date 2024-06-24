package net.neoforged.gradle.common.util;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * Utility class for dealing with classpaths and their entries.
 */
public class ClasspathUtils {

    private ClasspathUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    public static boolean isClasspathEntry(File entry) {
        return entry.getName().endsWith(".jar") || entry.getName().endsWith(".zip");
    }

    public static boolean isMinecraftClasspathEntry(File entry) {
        if (!isClasspathEntry(entry)) {
            return false;
        }

        //Check if the file contains the class:
        //net.minecraft.client.Minecraft
        //This is a class that is always present in the Minecraft jar.
        //This same heuristic is used by FML: https://github.com/neoforged/FancyModLoader/blob/89a32f970ba9137cb74e681af9cdaed0fc3e2085/loader/src/main/java/net/neoforged/fml/loading/targets/CommonUserdevLaunchHandler.java#L24
        try(final ZipFile zipFile = new ZipFile(entry)) {
            return zipFile.getEntry("net/minecraft/client/Minecraft.class") != null;
        } catch (IOException ignored) {
            return false;
        }

    }
}
