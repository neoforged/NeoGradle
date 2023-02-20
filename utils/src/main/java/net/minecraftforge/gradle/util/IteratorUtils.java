package net.minecraftforge.gradle.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class IteratorUtils {


    private IteratorUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: IteratorUtils. This is a utility class");
    }
    /**
     * Advances {@code iterator} to the end, returning the second to last element.
     *
     * @return the last element of {@code iterator}
     * @throws NoSuchElementException if the iterator is empty, or only contains a single element.
     */
    public static <T extends @Nullable Object> T getSecondToLast(Iterator<T> iterator) {
        T current = iterator.next();
        if (!iterator.hasNext()) {
            throw new NoSuchElementException("Iterator only contains a single element");
        }
        while(true) {
            current = iterator.next();
            if (!iterator.hasNext()) {
                return current;
            }
        }
    }
}
