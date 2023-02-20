package net.minecraftforge.gradle.util;

import javax.annotation.Nonnull;

/**
 * Util class for handling string capitalization
 */
public final class StringCapitalizationUtils {

    private StringCapitalizationUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: StringCapitalizationUtils. This is a utility class");
    }

    @Nonnull
    public static String capitalize(@Nonnull final String toCapitalize) {
        return toCapitalize.length() > 1 ? toCapitalize.substring(0, 1).toUpperCase() + toCapitalize.substring(1) : toCapitalize;
    }
}
