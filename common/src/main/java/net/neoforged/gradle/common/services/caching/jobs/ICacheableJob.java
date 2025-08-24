package net.neoforged.gradle.common.services.caching.jobs;

import com.machinezoo.noexception.throwing.ThrowingFunction;
import com.machinezoo.noexception.throwing.ThrowingSupplier;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Defines a job that can be cached.
 *
 * @param <I> The input type of the job.
 * @param <O> The type of the outputs of the job.
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
     * @return The outputs of the job.
     */
    List<OutputEntry> outputs();

    /**
     * Executes the job.
     *
     * @param input The input of the job.
     * @return The outputs of the job.
     */
    O execute(I input) throws Throwable;

    /**
     * The functional interface for a runnable that throws an exception.
     */
    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Represents a single outputs entry that will be cached.
     *
     * @param output The outputs that will be cached.
     * @param isDirectory Whether the outputs is a directory or a file.
     * @param createsDirectory Whether or not the directory is created by the caching step, ignored when isParameter is false.
     * @param mergesDirectory Whether or not the directory is merged by the caching step, and as such does not need a clean, ignored when isParameter is false.
     */
    record OutputEntry(File output, boolean isDirectory, boolean createsDirectory, boolean mergesDirectory) {
        public static OutputEntry file(File output) {
            return new OutputEntry(output, false, false, false);
        }

        public static OutputEntry directory(File output) {
            return new OutputEntry(output, true, false, false);
        }

        public static OutputEntry createsDirectory(File output) {
            return new OutputEntry(output, true, true, false);
        }

        public static OutputEntry mergesDirectory(File output) {
            return new OutputEntry(output, true, false, true);
        }
    }

    /**
     * Creates a new cacheable job that executes the given code.
     *
     * @param outputs           The outputs of the job.
     * @param execute          The code to execute.
     */
    record Default(List<OutputEntry> outputs, ThrowingRunnable execute) implements ICacheableJob<Void, Void> {

        public static Default of(ThrowingRunnable execute, OutputEntry... entries) {
            return new Default(List.of(entries), execute);
        }

        /**
         * Creates a new cacheable job that executes the given code for the file provided by the property.
         * Realising the property when this method is called.
         *
         * @param execute The code to execute.
         * @param output  The outputs of the job.
         * @return The created job.
         */
        public static Default file(ThrowingRunnable execute, RegularFileProperty... output) {
            return new Default(Arrays.stream(output).map(RegularFileProperty::getAsFile).map(Provider::get).map(OutputEntry::file).toList(), execute);
        }

        /**
         * Creates a new cacheable job that executes the given code for the file provided by the property.
         * Realising the property when this method is called.
         *
         * @param execute The code to execute.
         * @param output  The outputs of the job.
         * @return The created job.
         */
        public static Default file(ThrowingRunnable execute, List<RegularFileProperty> output) {
            return new Default(output.stream().map(RegularFileProperty::getAsFile).map(Provider::get).map(OutputEntry::file).toList(), execute);
        }

        /**
         * Creates a new cacheable job that executes the given code for the directory provided by the property.
         * Realising the property when this method is called.
         *
         * @param output The outputs of the job.
         * @param execute The code to execute.
         * @return The created job.
         */
        public static Default directory(DirectoryProperty output, ThrowingRunnable execute) {
            return new Default(List.of(OutputEntry.createsDirectory(output.get().getAsFile())), execute);
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
     * @param outputs The outputs of the job.
     * @param execute The code to execute.
     * @param <V> The type of the outputs of the job.
     */
    record Initial<V>(String name, List<OutputEntry> outputs, ThrowingSupplier<V> execute) implements ICacheableJob<Void, V> {

        /**
         * Creates a new cacheable job that executes the given code for the file provided by the property.
         * Realising the property when this method is called.
         *
         * @param name    The name of the job.
         * @param execute The code to execute.
         * @param output  The outputs of the job.
         * @return The created job.
         */
        public static <V> Initial<V> file(String name, ThrowingSupplier<V> execute, RegularFileProperty... output) {
            return new Initial<>(name, Arrays.stream(output).map(RegularFileProperty::getAsFile).map(Provider::get).map(OutputEntry::file).toList(), execute);
        }

        /**
         * Creates a new cacheable job that executes the given code for the directory provided by the property.
         * Realising the property when this method is called.
         *
         * @param name The name of the job.
         * @param output The outputs of the job.
         * @param execute The code to execute.
         * @return The created job.
         */
        public static <V> Initial<V> directory(String name, Provider<Directory> output, ThrowingSupplier<V> execute) {
            return new Initial<>(name, List.of(OutputEntry.createsDirectory(output.get().getAsFile())), execute);
        }

        /**
         * Creates a new cacheable job that executes the given code for the directory provided by the property, merging the outputs, without deleting what was already there.
         * Realising the property when this method is called.
         *
         * @param name The name of the job.
         * @param output The outputs of the job.
         * @param execute The code to execute.
         * @return The created job.
         */
        public static <V> Initial<V> merging(String name, Provider<Directory> output, ThrowingSupplier<V> execute) {
            return new Initial<>(name, List.of(OutputEntry.mergesDirectory(output.get().getAsFile())), execute);
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
     * @param outputs The outputs of the job.
     * @param job The job to stage.
     * @param <I> The input type of the job.
     * @param <O> The outputs type of the job.
     */
    record Staged<I, O>(String name, List<OutputEntry> outputs, ThrowingFunction<I, O> job) implements ICacheableJob<I, O> {

        /**
         * Creates a new cacheable job that executes the given code for the file provided by the property.
         * Realising the property when this method is called.
         *
         * @param <U>     The input type of the job.
         * @param <V>     The outputs type of the job.
         * @param name    The name of the stage.
         * @param execute The code to execute.
         * @param output  The outputs of the job.
         * @return The created job.
         */
        public static <U,V> Staged<U, V> file(String name, ThrowingFunction<U, V> execute, RegularFileProperty... output) {
            return new Staged<>(name, Arrays.stream(output).map(RegularFileProperty::getAsFile).map(Provider::get).map(OutputEntry::file).toList(), execute);
        }

        /**
         * Creates a new cacheable job that executes the given code for the file provided by the property.
         * Realising the property when this method is called.
         *
         * @param <U>     The input type of the job.
         * @param <V>     The outputs type of the job.
         * @param name    The name of the stage.
         * @param execute The code to execute.
         * @param output  The outputs of the job.
         * @return The created job.
         */
        public static <U,V> Staged<U, V> file(String name, ThrowingSupplier<V> execute, RegularFileProperty... output) {
            return new Staged<>(name, Arrays.stream(output).map(RegularFileProperty::getAsFile).map(Provider::get).map(OutputEntry::file).toList(), u -> execute.get());
        }

        /**
         * Creates a new cacheable job that executes the given code for the file provided by the property.
         * Realising the property when this method is called.
         *
         * @param <U>     The input type of the job.
         * @param <V>     The outputs type of the job.
         * @param name    The name of the stage.
         * @param execute The code to execute.
         * @param output  The outputs of the job.
         * @return The created job.
         */
        public static <U,V> Staged<U, V> file(String name, ThrowingRunnable execute, RegularFileProperty... output) {
            return new Staged<>(name, Arrays.stream(output).map(RegularFileProperty::getAsFile).map(Provider::get).map(OutputEntry::file).toList(), u -> {
                execute.run();
                return null;
            });
        }

        @Override
        public O execute(I input) throws Throwable {
            return job.apply(input);
        }
    }
}
