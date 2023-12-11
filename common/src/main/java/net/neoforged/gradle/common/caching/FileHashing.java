package net.neoforged.gradle.common.caching;

import net.neoforged.gradle.util.HashFunction;
import org.apache.commons.io.output.NullOutputStream;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides in-memory caching of file-hashes based on last-modification time and size.
 */
class FileHashing {
    private final Map<Path, CachedHash> cachedHashes = new ConcurrentHashMap<>();

    public byte[] getMd5Hash(Path path) {
        return cachedHashes.compute(path, CachedHash::compute).hashValue;
    }

    private static final class CachedHash {
        private final long lastModified;
        private final long fileSize;
        private final byte[] hashValue;

        public static CachedHash compute(Path path, @Nullable CachedHash cachedHash) {
            try {
                // Instead of reading size + last modified separately, we use this function to make race conditions
                // less likely. We still don't know if the underlying OS APIs return this information atomically,
                // but if they do, we at least make use of that fact.
                BasicFileAttributes attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class)
                        .readAttributes();
                long lastModified = attributes.lastModifiedTime().toMillis();
                long fileSize = attributes.size();

                if (cachedHash != null && cachedHash.lastModified == lastModified && cachedHash.fileSize == fileSize) {
                    return cachedHash;
                }

                // Compute the digest in a streaming fashion without reading the full file into memory
                MessageDigest digest = HashFunction.MD5.get();
                try (DigestOutputStream out = new DigestOutputStream(NullOutputStream.NULL_OUTPUT_STREAM, digest)) {
                    Files.copy(path, out);
                }

                return new CachedHash(digest.digest(), lastModified, fileSize);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public CachedHash(byte[] hashValue, long lastModified, long fileSize) {
            this.hashValue = hashValue;
            this.lastModified = lastModified;
            this.fileSize = fileSize;
        }
    }
}
