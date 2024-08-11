package net.neoforged.gradle.common.services.caching.hasher;

import net.neoforged.gradle.common.services.caching.logging.CacheLogger;
import net.neoforged.gradle.common.util.hash.HashCode;
import net.neoforged.gradle.common.util.hash.HashFunction;
import net.neoforged.gradle.common.util.hash.Hasher;
import net.neoforged.gradle.common.util.hash.Hashing;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskInputs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class TaskHasher {
    private final HashFunction hashFunction = Hashing.sha256();
    private final Hasher hasher = hashFunction.newHasher();

    private final Task task;
    private final CacheLogger logger;

    public TaskHasher(Task task, CacheLogger logger) {
        this.task = task;
        this.logger = logger;
    }

    public void hash() throws IOException {
        logger.debug("Hashing task: " + task.getPath());
        hasher.putString(task.getClass().getName());

        final TaskInputs taskInputs = task.getInputs();
        hash(taskInputs);
    }

    private void hash(TaskInputs inputs) throws IOException {
        logger.debug("Hashing task inputs: " + task.getPath());
        inputs.getProperties().forEach((key, value) -> {
            logger.debug("Hashing task input property: " + key);
            hasher.putString(key);
            logger.debug("Hashing task input property value: " + value);
            hasher.put(value, false); //We skin unknown types (mostly file collections)
        });

        final Set<File> inputFiles = new HashSet<>();

        for (File file : inputs.getFiles()) {
            try (Stream<Path> pathStream = Files.walk(file.toPath())) {
                for (Path path : pathStream.filter(Files::isRegularFile).toList()) {
                    inputFiles.add(path.toFile());
                }
            }
        }

        final List<File> files = new ArrayList<>(inputFiles);
        files.sort(Comparator.comparing(File::getAbsolutePath));

        for (File file : files) {
            logger.debug("Hashing task input file: " + file.getAbsolutePath());
            hasher.putString(file.getName());
            final HashCode code = hashFunction.hashFile(file);
            logger.debug("Hashing task input file hash: " + code);
            hasher.putHash(code);
        }
    }

    public HashCode create() throws IOException {
        hash();
        return hasher.hash();
    }
}
