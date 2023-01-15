package net.minecraftforge.gradle.base.util;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import org.gradle.api.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Consumer;

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

    public static <V> Transformer<V, V> peak(Consumer<V> peakConsumer) {
        return t -> {
            peakConsumer.accept(t);
            return t;
        };
    }

    public static <V> ThrowingTransformer<V, V> peakWithThrow(ThrowingConsumer<V> peakConsumer) {
        return t -> {
            peakConsumer.apply(t);
            return t;
        };
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
