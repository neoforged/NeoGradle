package net.neoforged.gradle.userdev.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class ListUtils
{

    public static <E> int removeIfAndReturnIndex(List<E> collection, Predicate<E> filter) {
        Objects.requireNonNull(filter);
        Objects.requireNonNull(collection);
        boolean removed = false;

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
}
