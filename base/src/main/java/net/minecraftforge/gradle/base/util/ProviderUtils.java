package net.minecraftforge.gradle.base.util;

import org.gradle.api.NamedDomainObjectCollection;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ProviderUtils {

    private ProviderUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: ProviderUtils. This is a utility class");
    }

    public static  <T> Provider<Collection<T>> getNamedCollectionEntriesAsProvider(final Project project, final NamedDomainObjectCollection<T> collection) {
        return project.provider(() -> collection);
    }

    public static <T> Provider<List<T>> reduceListProviders(final Provider<List<T>> left, final Provider<List<T>> right) {
        return left.zip(right, (l, r) -> {
            final List<T> result = new ArrayList<>(l);
            result.addAll(r);
            return result;
        });
    }

    public static <T> Provider<List<T>> reduceListProperties(final ListProperty<T> left, final ListProperty<T> right) {
        return left.zip(right, (l, r) -> {
            final List<T> result = new ArrayList<>(l);
            result.addAll(r);
            return result;
        });
    }
}
