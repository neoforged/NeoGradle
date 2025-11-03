package net.neoforged.gradle.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReflectionUtils {
    /**
     * Reflectively sets a final private field on an object.
     *
     * @param target    The object whose field should be set
     * @param fieldName The name of the field
     * @param value     The new value to set
     * @throws ReflectiveOperationException if the field cannot be set
     */
    public static void setFinalField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Class<?> clazz = target.getClass();
        java.lang.reflect.Field field = null;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException("Field '" + fieldName + "' not found in class hierarchy.");
        }
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Calls setFinalField but wraps all exceptions in a RuntimeException.
     * This is useful for calling from constructors where checked exceptions are not allowed.
     *
     * @param target    The object whose field should be set
     * @param fieldName The name of the field
     * @param value     The new value to set
     */
    public static void setFinalFieldUnchecked(Object target, String fieldName, Object value) {
        try {
            setFinalField(target, fieldName, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set final field '" + fieldName + "'", e);
        }
    }

    public static void setFinalFieldUncheckedWithAlternatives(Object target, Object value, String... fieldNames) {
        final List<ReflectiveOperationException> exceptions = new ArrayList<>(fieldNames.length);
        for (final String fieldName : fieldNames)
        {
            try {
                setFinalField(target, fieldName, value);
                return;
            } catch (ReflectiveOperationException e) {
                exceptions.add(e);
            }
        }

        final RuntimeException ex = new RuntimeException("Failed to set final field '" + String.join(", ", fieldNames) + "'");
        for (final ReflectiveOperationException exception : exceptions)
        {
            ex.addSuppressed(exception);
        }

        throw ex;
    }
}
