package net.neoforged.gradle.common.util;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class ListUtils
{

    public static <E> int removeIfAndReturnIndex(List<E> collection, Predicate<E> filter) {
        Objects.requireNonNull(filter);
        Objects.requireNonNull(collection);

        int index = -1;
        for (int i = 0; i < collection.size(); i++)
        {
            if (filter.test(collection.get(i))) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            collection.remove(index);
        }

        return index;
    }

    public static <E> @Nullable E find(List<E> collection, Predicate<E> filter) {
        for (final E e : collection)
        {
            if (filter.test(e))
                return e;
        }

        return null;
    }
}
