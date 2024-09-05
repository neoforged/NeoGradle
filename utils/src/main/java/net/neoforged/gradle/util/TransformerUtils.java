package net.neoforged.gradle.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.SourceSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class which handles gradles transformers.
 */
public final class TransformerUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformerUtils.class);

    private TransformerUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: TransformerUtils. This is a utility class");
    }

    /**
     * Creates a transformer whose execution is guarded by a try-catch block, without callbacks.
     *
     * @param toGuard The transformer with exceptions to guard
     * @return The guarded transformer
     *
     * @param <T> The type of the input to the transformer
     * @param <V> The type of the output of the transformer
     */
    public static <V, T> Transformer<V, T> guard(final ThrowingTransformer<V, T> toGuard) {
        return t -> {
            try {
                //noinspection ConstantConditions - We are allowed to return null here. It is not a problem.
                return toGuard.transform(t);
            } catch (Throwable e) {
                LOGGER.error("Failed to transform: " + t, e);
                throw new RuntimeException("Failed to transform: " + t, e);
            }
        };
    }

    /**
     * Creates a transformer whose execution is guarded by a try-catch block, without callbacks.
     *
     * @param toGuard The transformer with exceptions to guard
     * @return The guarded transformer
     *
     * @param <T> The type of the input to the transformer
     * @param <V> The type of the output of the transformer
     */
    public static <V, T, S extends AutoCloseable> Transformer<V, T> guardWithResource(final ThrowingTransformer<V, S> toGuard, ThrowingFunction<T, S> factory) {
        return t -> {
            try(final S closeable = factory.apply(t)) {
                //noinspection ConstantConditions - We are allowed to return null here. It is not a problem.
                return toGuard.transform(closeable);
            } catch (Throwable e) {
                LOGGER.error("Failed to transform: " + t, e);
                throw new RuntimeException("Failed to transform: " + t, e);
            }
        };
    }

    /**
     * Creates a transformer whose execution is guarded by a try-catch block, with a callback for the exception.
     *
     * @param toGuard The transformer with exceptions to guard
     * @return The guarded transformer
     *
     * @param <T> The type of the input to the transformer
     * @param <V> The type of the output of the transformer
     */
    public static <V, T>Transformer<V, T> guard(final ThrowingTransformer<V, T> toGuard, final Consumer<Throwable> onFailure) {
        return t -> {
            try {
                //noinspection ConstantConditions - We are allowed to return null here. It is not a problem.
                return toGuard.transform(t);
            } catch (Throwable e) {
                onFailure.accept(e);
                LOGGER.error("Failed to transform: " + t, e);
                throw new RuntimeException("Failed to transform: " + t, e);
            }
        };
    }

    /**
     * Creates a transformer whose execution is guarded by a try-catch-finally block, with callbacks.
     *
     * @param toGuard The transformer with exceptions to guard
     * @param before The callback to run before the guarded transformer is executed
     * @param after The callback to run after the guarded transformer is executed
     * @param onFailure The callback to run if the guarded transformer throws an exception
     * @param finalizer The callback to run after the guarded transformer is executed, regardless of whether it threw an exception
     * @return The guarded transformer
     *
     * @param <T> The type of the input to the transformer
     * @param <V> The type of the output of the transformer
     */
    public static <V, T> Transformer<V, T> guard(final ThrowingTransformer<V, T> toGuard, final Runnable before, final Consumer<V> after, final Consumer<Throwable> onFailure, final Runnable finalizer) {
        return t -> {
            try {
                before.run();
                V ret = toGuard.transform(t);
                after.accept(ret);
                //noinspection ConstantConditions - We are allowed to return null here. It is not a problem.
                return ret;
            } catch (Throwable e) {
                onFailure.accept(e);
                LOGGER.error("Failed to transform: " + t, e);
                throw new RuntimeException("Failed to transform: " + t, e);
            } finally {
                finalizer.run();
            }
        };
    }

    /**
     * Creates a transformer which will execute a callback on the input before passing it back as a result.
     * The consumer can not throw an exception.
     *
     * @param peakConsumer The callback to execute on the input
     * @param <V> The type of the output of the transformer
     */
    public static <V> Transformer<V, V> peak(Consumer<V> peakConsumer) {
        return t -> {
            peakConsumer.accept(t);
            return t;
        };
    }

    /**
     * Creates a transformer which will execute a callback on the input before passing it back as a result.
     * The consumer can throw an exception.
     *
     * @param peakConsumer The callback to execute on the input
     * @param <V> The type of the output of the transformer
     */
    public static <V> ThrowingTransformer<V, V> peakWithThrow(ThrowingConsumer<V> peakConsumer) {
        return t -> {
            peakConsumer.apply(t);
            return t;
        };
    }
    
    public static <V extends FileSystemLocation> Transformer<V, V> ensureExists() {
        return guard(t -> {
            if (!t.getAsFile().exists()) {
                if (t instanceof Directory) {
                    t.getAsFile().mkdirs();
                } else {
                    t.getAsFile().createNewFile();
                }
                
            }
            return t;
        });
    }

    /**
     * Creates a transformer which will combine all the values into a single map.
     *
     * @param project The project to use for creating the map property
     * @param keyClass The class of the key of the map
     * @param valueClass The class of the value of the map
     * @param valueProvider The function to provide the map for each input
     * @return The transformer which will combine all the maps into a single map
     * @param <K> The type of the key of the map
     * @param <V> The type of the value of the map
     * @param <I> The type of the input to the transformer
     * @param <C> The type of the collection of inputs
     */
    public static <K, V, I, C extends List<I>> Transformer<? extends Provider<Map<K, V>>, C> combineAllMaps(final Project project, final Class<K> keyClass, final Class<V> valueClass, final Function<I, Provider<Map<K, V>>> valueProvider) {
        final MapProperty<K, V> map = project.getObjects().mapProperty(keyClass, valueClass);
        return guard(t -> {
            for (I i : t) {
                map.putAll(valueProvider.apply(i));
            }
            return map;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <K, V, I, C extends List<I>> Transformer<? extends Provider<Multimap<K, V>>, C> combineAllMultiMaps(final Project project, final Class<K> keyClass, final Class<V> valueClass, final Function<I, Provider<Multimap<K, V>>> valueProvider) {
        record Entry<K, V>(K key, V value) {}
        final ListProperty<Entry<K, V>> map = (ListProperty) project.getObjects().listProperty(Entry.class);
        return guard(t -> {
            for (I i : t) {
                map.addAll(valueProvider.apply(i).map(m -> m.entries().stream().map(e -> new Entry<>(e.getKey(), e.getValue())).toList()));
            }
            return map.map(entries -> {
                final Multimap<K, V> multimap = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);
                for (Entry<K, V> entry : entries) {
                    multimap.put(entry.key, entry.value);
                }
                return multimap;
            });
        });
    }


    /**
     * Creates a transformer which will execute a callback on the inputs last value before passing it back as a result.
     * @param project The project to use for creating the provider
     * @param valueProvider The function to provide the value for the last input
     * @return The transformer which will provide the value for the last input
     * @param <V> The type of the output of the transformer
     * @param <I> The type of the input to the transformer
     * @param <C> The type of the collection of inputs
     */
    public static <V, I, C extends List<I>> Transformer<? extends Provider<V>, C> takeLast(final Project project, final Function<I, Provider<V>> valueProvider) {
        return guard(t -> {
            Provider<V> result = project.provider(() -> null);
            if (t.isEmpty())
                return result;

            for (int i = t.size() - 1; i >= 0; i--) {
                result = result.orElse(valueProvider.apply(t.get(i)));
            }

            return result;
        });
    }

    /**
     * Creates a transformer which will combine all the values into a single list.
     *
     * @param project The project to use for creating the list property
     * @param valueClass The class of the value of the list
     * @param valueProvider The function to provide the list for each input
     * @return The transformer which will combine all the lists into a single list
     * @param <V> The type of the value of the list
     * @param <I> The type of the input to the transformer
     * @param <C> The type of the collection of inputs
     */
    public static <V, I, C extends List<I>> Transformer<Provider<List<V>>, C> combineAllLists(final Project project, Class<V> valueClass, Function<I, Provider<List<V>>> valueProvider) {
        return guard(t -> {
            final ListProperty<V> values = project.getObjects().listProperty(valueClass);
            for (I i : t) {
                values.addAll(valueProvider.apply(i));
            }
            return values;
        });
    }

    /**
     * Creates a transformer which will combine all the values into a single set.
     *
     * @param project The project to use for creating the set property
     * @param valueClass The class of the value of the set
     * @param valueProvider The function to provide the set for each input
     * @return The transformer which will combine all the sets into a single set
     * @param <V> The type of the value of the set
     * @param <I> The type of the input to the transformer
     * @param <C> The type of the collection of inputs
     */
    public static <V, I, C extends List<I>> Transformer<Provider<Set<V>>, C> combineAllSets(final Project project, Class<V> valueClass, Function<I, Provider<Set<V>>> valueProvider) {
        return guard(t -> {
            final SetProperty<V> values = project.getObjects().setProperty(valueClass);
            for (I i : t) {
                values.addAll(valueProvider.apply(i));
            }
            return values;
        });
    }

    public static <I, C extends Collection<I>> Transformer<? extends ConfigurableFileCollection, C> combineFileCollections(final Project project, Function<I, ConfigurableFileCollection> valueProvider) {
        return guard(t -> {
            final ConfigurableFileCollection files = project.files();
            for (I i : t) {
                files.from(valueProvider.apply(i));
            }
            return files;
        });
    }

    /**
     * Creates a transformed provider that returns a list of values if the predicate is true.
     *
     * @param predicate The predicate to check
     * @param whenTrue The value to return if the predicate is true
     * @return A transformed provider if the predicate is true, otherwise null
     * @param <V> The type of the value to return
     */
    @SafeVarargs
    public static <V> Provider<? extends List<V>> ifTrue(Provider<Boolean> predicate, Provider<? extends V>... whenTrue) {
        if (whenTrue.length == 0) {
            return predicate.map(p -> List.of());
        }

        if (whenTrue.length == 1) {
            return whenTrue[0].zip(predicate, (v, p) -> p ? List.of(v) : List.of());
        }

        Provider<? extends List<V>> zippedArray = whenTrue[0].zip(predicate, (v, p) -> p ? List.of(v) : List.of());
        for (int i = 1; i < whenTrue.length; i++) {
            zippedArray = zippedArray.zip(
                    whenTrue[i].zip(predicate, (v, p) -> p ? List.of(v) : List.of()),
                    (BiFunction<List<V>, List<V>, List<V>>) (vs, objects) -> {
                        final ArrayList<V> ret = new ArrayList<>(vs);
                        ret.addAll(objects);
                        return ret;
                    }
            );
        }

        return zippedArray;
    }

    /**
     * Creates a transformed provider that returns a list of values if the predicate is true.
     *
     * @param predicate The predicate to check
     * @param whenTrue The value to return if the predicate is true
     * @return A transformed provider if the predicate is true, otherwise null
     * @param <V> The type of the value to return
     */
    public static <V> Provider<? extends List<V>> ifTrue(Provider<Boolean> predicate, Provider<? extends Collection<V>> whenTrue) {
        return predicate.zip(whenTrue, (p, v) -> p ? List.copyOf(v) : List.of());
    }

    /**
     * Creates a transformed provider that returns a list of values if the predicate is true.
     *
     * @param predicate The predicate to check
     * @param whenTrue The value to return if the predicate is true
     * @return A transformed provider if the predicate is true, otherwise null
     * @param <V> The type of the value to return
     */
    public static <V> Provider<? extends List<V>> ifTrue(Boolean predicate, Provider<? extends Collection<V>> whenTrue) {
        return whenTrue.map(v -> predicate ? List.copyOf(v) : List.of());
    }

    /**
     * Creates a transformed provider that returns a list of values if the predicate is true.
     *
     * @param predicate The predicate to check
     * @param whenTrue The value to return if the predicate is true
     * @return A transformed provider if the predicate is true, otherwise null
     * @param <V> The type of the value to return
     */
    @SafeVarargs
    public static <V> Provider<? extends List<V>> ifTrue(Provider<Boolean> predicate, V... whenTrue) {
        if (whenTrue.length == 0) {
            return predicate.map(p -> List.of());
        }

        return predicate.map(p -> p ? List.of(whenTrue) : List.of());
    }

    /**
     * Creates a transformed provider that returns a map of with the key value pair.
     *
     * @param predicate The predicate to check
     * @param keyWhenTrue The key to return if the predicate is true
     * @param valueWhenTrue The value to return if the predicate is true
     * @return A transformed provider if the predicate is true, otherwise null
     * @param <K> The type of the key to return
     * @param <V> The type of the value to return
     */
    public static <K, V> Provider<? extends Map<K, V>> ifTrueMap(Provider<Boolean> predicate, K keyWhenTrue, V valueWhenTrue) {
        return predicate.map(p -> p ? Map.of(keyWhenTrue, valueWhenTrue) : Map.of());
    }

    @SafeVarargs
    public static Transformer<Provider<Boolean>, Boolean> and(Provider<Boolean>... rightProvider) {
        if (rightProvider.length == 0) {
            throw new IllegalStateException("No right provider provided");
        }

        if (rightProvider.length == 1) {
            return left -> rightProvider[0].map(o -> left && o);
        }

        return inputBoolean -> {
            Provider<Boolean> result = rightProvider[0].map(o -> inputBoolean && o);
            for (int i = 1; i < rightProvider.length; i++) {
                result = result.zip(rightProvider[i], (l, r) -> l && r);
            }
            return result;
        };
    }

    @SafeVarargs
    public static Provider<Boolean> or(Boolean initial, Provider<Boolean>... rightProvider) {
        if (rightProvider.length == 0) {
            throw new IllegalStateException("No right provider provided");
        }

        if (rightProvider.length == 1) {
            return rightProvider[0].map(o -> initial || o);
        }

        Provider<Boolean> input = rightProvider[0].map(o -> initial || o);
        for (int i = 1; i < rightProvider.length; i++) {
            input = input.zip(rightProvider[i], (l, r) -> l || r);
        }
        return input;
    }

    @SafeVarargs
    public static Transformer<Provider<Boolean>, Boolean> or(Provider<Boolean>... rightProvider) {
        return inputBoolean -> or(inputBoolean, rightProvider);
    }

    /**
     * This method is a short-cut to fix a compiler error:
     * <p>
     *     When using the {@link Provider#orElse(T)} method, the compiler can not infer the type of the object,
     *     and for some reason returns a {@link Provider<Object>} instead of the expected type: {@link Provider<T>}.
     *     This method is a short-cut to fix that issue.
     *     <br>
     *     <b>NOTE:</b> This method is not needed in all cases, only when the compiler can not infer the type.
     * </p>
     * @param provider The provider to get the value from
     * @param value The value to return if the provider is empty
     * @return The value of the provider, or the default value if the provider is empty
     * @param <T> The type of the value
     */
    public static <T> Provider<T> defaulted(Provider<T> provider, T value) {
        return provider.orElse(value);
    }

    /**
     * This method is a short-cut to fix a compiler error:
     * <p>
     *     When using the {@link Provider#orElse(T)} method, the compiler can not infer the type of the object,
     *     and for some reason returns a {@link Provider<Object>} instead of the expected type: {@link Provider<T>}.
     *     This method is a short-cut to fix that issue.
     *     <br>
     *     <b>NOTE:</b> This method is not needed in all cases, only when the compiler can not infer the type.
     * </p>
     * @param provider The provider to get the value from
     * @param value The value to return if the provider is empty
     * @return The value of the provider, or the default value if the provider is empty
     * @param <T> The type of the value
     */

    public static <T> Provider<T> lazyDefaulted(Provider<T> provider, Provider<T> value) {
        return provider.orElse(value);
    }

    /**
     * A definition for a transformer which can throw an exception.
     *
     * @param <V> The type of the output of the transformer.
     * @param <T> The type of the input to the transformer.
     */
    @FunctionalInterface
    public interface ThrowingTransformer<V, T> {
        /**
         * Invoked to execute the transformer on an instance of T.
         * Is only invoked if the instance is not null.
         *
         * @param t The instance to transform
         * @return The transformed instance
         * @throws Throwable If an exception occurs during the transformation
         */
        @Nullable
        V transform(@NotNull T t) throws Throwable;
    }

    /**
     * A definition for a function which can throw an exception.
     * @param <T> The input type of the function
     * @param <S> The output type of the function
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, S> {
        /**
         * Invoked to execute the function on an instance of T.
         * @param t The instance to execute on.
         * @return The result of the execution.
         * @throws Throwable If an exception occurs during the execution.
         */
        S apply(T t) throws Throwable;
    }

    /**
     * A definition for a consumer which can throw an exception.
     * @param <S> The input type of the function
     */
    @FunctionalInterface
    public interface ThrowingConsumer<S> {
        /**
         * Invoked to execute the consumer on an instance of S.
         * @param s The instance to execute on.
         * @throws Throwable If an exception occurs during the execution.
         */
        void apply(S s) throws Throwable;
    }
}
