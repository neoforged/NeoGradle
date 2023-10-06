package net.neoforged.gradle.platform.util;

public class StringUtils {
    
    public static String getSlicedPrefixSection(final String input, final String separator, final int toCut) {
        final String[] split = input.split(separator);
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < split.length - toCut; i++) {
            builder.append(split[i]);
            builder.append(separator);
        }
        return builder.toString();
        
    }
}
