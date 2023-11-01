package net.neoforged.gradle.common.util;

public class PathUtils {
    
    private PathUtils() {
        throw new IllegalStateException("Tried to create utility class!");
    }
    
    public static String quote(final String path) {
        return "\"" + path + "\"";
    }
}
