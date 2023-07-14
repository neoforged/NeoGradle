package net.minecraftforge.gradle.legacy.util;

import java.lang.reflect.Field;

public final class ReflectionUtils {

    private ReflectionUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ReflectionUtils. This is a utility class");
    }

    public static Object getFieldContent(Object source, String name) {
        final Field field;
        try {
            field = source.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(true);
        try {
            return field.get(source);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
