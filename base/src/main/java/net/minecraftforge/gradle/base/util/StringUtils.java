package net.minecraftforge.gradle.base.util;

import javax.annotation.Nonnull;

public final class StringUtils {

    private StringUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: StringUtils. This is a utility class");
    }

    @Nonnull
    public static String capitalize(@Nonnull final String toCapitalize) {
        return toCapitalize.length() > 1 ? toCapitalize.substring(0, 1).toUpperCase() + toCapitalize.substring(1) : toCapitalize;
    }
}
