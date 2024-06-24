package net.neoforged.gradle.util;

import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Util class for handling string capitalization
 */
public final class StringCapitalizationUtils {

    private StringCapitalizationUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: StringCapitalizationUtils. This is a utility class");
    }

    @Nonnull
    public static String capitalize(@Nonnull final String toCapitalize) {
        return toCapitalize.length() > 1 ? toCapitalize.substring(0, 1).toUpperCase(Locale.ROOT) + toCapitalize.substring(1) : toCapitalize;
    }

    @Nonnull
    public static String deCapitalize(@Nonnull final String toCapitalize) {
        return toCapitalize.length() > 1 ? toCapitalize.substring(0, 1).toLowerCase(Locale.ROOT) + toCapitalize.substring(1) : toCapitalize;
    }
}
