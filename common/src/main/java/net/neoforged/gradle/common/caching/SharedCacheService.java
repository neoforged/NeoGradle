package net.neoforged.gradle.common.caching;

import net.neoforged.gradle.util.FileUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Abstraction for a shared cache that should be used for artifact produced by slow operations.
 * Preferably, since this does not support hashing the classpath, use this only if the
 * tool to produce the artifact is external and uniquely identified by a Maven coordinate.
 */
public abstract class SharedCacheService implements BuildService<SharedCacheService.Params> {

    private static final String EXTENSION_IN = "in";
    private static final String EXTENSION_OUT = "out";

    private final FileHashing fileHashing = new FileHashing();

    public CacheKeyBuilder cacheKeyBuilder(Project project) {
        return new CacheKeyBuilder(project);
    }

    public boolean cacheOutput(Project project, CacheKey cacheKey, Path output, IORunnable producer) throws IOException {

        Logger logger = project.getLogger();

        if (!getParameters().getEnabled().get()) {
            logger.debug("Skipping cache for {}, since it's disabled", output);
            producer.run();
            return false;
        }

        output = output.toAbsolutePath();

        Path cacheDir = getCacheDir();
        Path cachePath = cacheDir.resolve(cacheKey.asPath(EXTENSION_OUT));
        if (Files.isReadable(cachePath)) {
            logger.lifecycle("Reusing cached file {} for {}", cachePath, output);
            Files.setLastModifiedTime(cachePath, FileTime.from(Instant.now())); // Touch to keep track of unused files
            Files.copy(cachePath, output, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }

        producer.run();

        // If producer didn't throw, cache the output. Do this conservatively, since they might be on different file systems
        FileUtils.atomicCopy(output, cachePath);
        // This is for debugging only
        try (ByteArrayInputStream bis = new ByteArrayInputStream(cacheKey.getSourceMaterial().getBytes(StandardCharsets.UTF_8))) {
            FileUtils.atomicCopy(bis, cacheDir.resolve(cacheKey.asPath(EXTENSION_IN)));
        }
        return false;
    }

    private Path getCacheDir() {
        return getParameters().getCacheDirectory().getAsFile().get().toPath();
    }

    @FunctionalInterface
    public interface IORunnable {
        void run() throws IOException;
    }

    /**
     * Builder for cache keys.
     */
    public class CacheKeyBuilder {
        private final Path rootPath;
        private final Path gradleUserHomeRoot;

        @Nullable
        private String cacheDomain;

        @Nullable
        private Path tool;

        private final Set<Path> inputFiles = new HashSet<>();

        private final List<String> args = new ArrayList<>();

        public CacheKeyBuilder(Project project) {
            rootPath = project.getRootDir().toPath().toAbsolutePath();
            gradleUserHomeRoot = project.getGradle().getGradleUserHomeDir().toPath().toAbsolutePath();
        }

        public CacheKeyBuilder cacheDomain(@Nullable String cacheDomain) {
            this.cacheDomain = cacheDomain;
            return this;
        }

        public CacheKeyBuilder tool(Path path) {
            this.tool = path.toAbsolutePath();
            return this;
        }

        public CacheKeyBuilder inputFiles(Collection<File> files) {
            files.stream().map(f -> f.toPath().toAbsolutePath()).forEach(inputFiles::add);
            return this;
        }

        public CacheKeyBuilder argument(String arg) {
            this.args.add(arg);
            return this;
        }

        public CacheKeyBuilder arguments(Collection<String> args) {
            this.args.addAll(args);
            return this;
        }

        /**
         * Convenience method to apply a customization function to this builder as part of a
         * chained method call.
         */
        public CacheKeyBuilder apply(Consumer<CacheKeyBuilder> consumer) {
            consumer.accept(this);
            return this;
        }

        public CacheKey build() {
            HashCodeBuilder hasher = new HashCodeBuilder(fileHashing);

            // Tool always comes first
            if (tool == null) {
                hasher.add("NO_TOOL");
            } else {
                hasher.add(tool);
            }

            // Then all input files
            inputFiles.parallelStream()
                    .map(p -> Pair.of(relativizeAndNormalizePath(p), fileHashing.getMd5Hash(p)))
                    .sorted(Comparator.comparing(Pair::getLeft))
                    .forEachOrdered(pair -> hasher.add(pair.getValue(), "INPUT-MD5(" + pair.getKey() + ") = " + Hex.encodeHexString(pair.getValue())));

            // Then program arguments
            Pattern projectDirPattern = makeFuzzyDirPattern(rootPath);
            Pattern userHomeDirPattern = makeFuzzyDirPattern(gradleUserHomeRoot);
            for (String arg : args) {
                // Try to make paths in the arguments relocatable
                // We cannot simply try to parse the argument as a path, since it may be a list of paths
                arg = projectDirPattern.matcher(arg).replaceAll("PROJECT_DIR");
                arg = userHomeDirPattern.matcher(arg).replaceAll("GRADLE_USER_HOME");
                hasher.add(arg);
            }

            return new CacheKey(cacheDomain, hasher.buildHashCode(), hasher.buildSourceMaterial());
        }

        /**
         * Creates a regular expression that will match the given path.
         * Each path separator may be repeated, and may use either slash or backslash form.
         */
        private Pattern makeFuzzyDirPattern(Path path) {
            String platformPath = path.toAbsolutePath().toString();
            return Pattern.compile(Arrays.stream(platformPath.split("[/\\\\]+"))
                    .map(Pattern::quote)
                    .collect(Collectors.joining("[/\\\\]+")));
        }

        private String relativizeAndNormalizePath(Path path) {
            if (path.startsWith(rootPath)) {
                return "PROJECT/" + normalizeStringify(rootPath.relativize(path));
            }
            if (path.startsWith(gradleUserHomeRoot)) {
                return "USER_HOME/" + normalizeStringify(gradleUserHomeRoot.relativize(path));
            }

            return normalizeStringify(path);
        }

        private String normalizeStringify(Path path) {
            // Use slashes even on Windows
            return path.toString().replace('\\', '/');
        }
    }

    public abstract static class Params implements BuildServiceParameters {
        /**
         * Defaults to true, but can be used to fully disable caching (both writing to and reading from).
         */
        public abstract Property<Boolean> getEnabled();

        /**
         * Can be used to customize where the shared cache is stored.
         */
        public abstract DirectoryProperty getCacheDirectory();

        @Inject
        public Params(Gradle gradle) {
            getEnabled().convention(true);
            Provider<Directory> defaultDir = gradle.getRootProject().getLayout().dir(
                    gradle.getRootProject().provider(() -> new File(gradle.getGradleUserHomeDir(), "caches/neoForm"))
            );
            getCacheDirectory().convention(defaultDir);
        }
    }

    public static void register(Project project, String name, Consumer<SharedCacheService.Params> configurer) {
        project.getGradle().getSharedServices().registerIfAbsent(
                name,
                SharedCacheService.class,
                spec -> configurer.accept(spec.getParameters())
        );
    }
}
