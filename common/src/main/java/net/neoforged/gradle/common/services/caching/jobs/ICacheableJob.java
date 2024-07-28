package net.neoforged.gradle.common.services.caching.jobs;

import com.machinezoo.noexception.throwing.ThrowingFunction;
import com.machinezoo.noexception.throwing.ThrowingSupplier;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;

import java.io.File;

/**
 * Defines a job that can be cached.
 *
 * @param <I> The input type of the job.
 * @param <O> The type of the output of the job.
 */
public interface ICacheableJob<I, O> {

    /**
     * @return The name of the job.
     * @implSpec The default name of a job is 'default'
     */
    default String name() {
        return "default";
    }

    /**
     * @return The output of the job.
     */
    File output();

    /**
     * Executes the job.
     *
     * @param input The input of the job.
     * @return The output of the job.
     */
    O execute(I input) throws Throwable;

    /**
     * @return True if the output is a directory.
     */
    boolean createsDirectory();

    /**
     * The functional interface for a runnable that throws an exception.
     */
    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Creates a new cacheable job that executes the given code.
     *
     * @param output           The output of the job.
     * @param createsDirectory True if the output is a directory.
     * @param execute          The code to execute.
     */
    record Default(File output, boolean createsDirectory, ThrowingRunnable execute) implements ICacheableJob<Void, Void> {

        /**
         * Creates a new cacheable job that executes the given code for the file provided by the property.
         * Realising the property when this method is called.
         *
         * @param output The output of the job.
         * @param execute The code to execute.
         * @return The created job.
         */
        public static Default file(RegularFileProperty output, ThrowingRunnable execute) {
            return new Default(output.get().getAsFile(), false, execute);
        }

        /**
         * Creates a new cacheable job that executes the given code for the directory provided by the property.
         * Realising the property when this method is called.
         *
         * @param output The output of the job.
         * @param execute The code to execute.
         * @return The created job.
         */
        public static Default directory(DirectoryProperty output, ThrowingRunnable execute) {
            return new Default(output.get().getAsFile(), true, execute);
        }

        @Override
        public Void execute(Void input) throws Exception {
            execute().run();
            return null;
        }
    }

    /**
     * Creates a new cacheable job that executes the given code.
     *
     * @param name The name of the job.
     * @param output The output of the job.
     * @param createsDirectory True if the output is a directory.
     * @param execute The code to execute.
     * @param <V> The type of the output of the job.
     */
    record Initial<V>(String name, File output, boolean createsDirectory, ThrowingSupplier<V> execute) implements ICacheableJob<Void, V> {

        /**
         * Creates a new cacheable job that executes the given code for the file provided by the property.
         * Realising the property when this method is called.
         *
         * @param name The name of the job.
         * @param output The output of the job.
         * @param execute The code to execute.
         * @return The created job.
         */
        public static <V> Initial<V> file(String name, RegularFileProperty output, ThrowingSupplier<V> execute) {
            return new Initial<>(name, output.get().getAsFile(), false, execute);
        }

        /**
         * Creates a new cacheable job that executes the given code for the directory provided by the property.
         * Realising the property when this method is called.
         *
         * @param name The name of the job.
         * @param output The output of the job.
         * @param execute The code to execute.
         * @return The created job.
         */
        public static <V> Initial<V> directory(String name, Provider<Directory> output, ThrowingSupplier<V> execute) {
            return new Initial<>(name, output.get().getAsFile(), true, execute);
        }

        @Override
        public V execute(Void input) throws Throwable {
            return execute().get();
        }
    }

    /**
     * Creates a new cacheable job that stages the given job.
     *
     * @param name The name of the job.
     * @param output The output of the job.
     * @param createsDirectory True if the output is a directory.
     * @param job The job to stage.
     * @param <I> The input type of the job.
     * @param <O> The output type of the job.
     */
    record Staged<I, O>(String name, File output, boolean createsDirectory, ThrowingFunction<I, O> job) implements ICacheableJob<I, O> {

        /**
         * Creates a new cacheable job that executes the given code for the file provided by the property.
         * Realising the property when this method is called.
         *
         * @param name The name of the stage.
         * @param output The output of the job.
         * @param execute The code to execute.
         * @return The created job.
         * @param <U> The input type of the job.
         * @param <V> The output type of the job.
         */
        public static <U,V> Staged<U, V> file(String name, RegularFileProperty output, ThrowingFunction<U, V> execute) {
            return new Staged<>(name, output.get().getAsFile(), false, execute);
        }

        @Override
        public O execute(I input) throws Throwable {
            return job.apply(input);
        }
    }
}
