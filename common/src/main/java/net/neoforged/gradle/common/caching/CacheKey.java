package net.neoforged.gradle.common.caching;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class CacheKey {
    private static final String CACHE_DOMAIN_ALL = "all";
    @Nullable
    private final String cacheDomain;
    private final String hashCode;
    private final String sourceMaterial;

    CacheKey(@Nullable String cacheDomain, String hashCode, String sourceMaterial) {
        this.cacheDomain = cacheDomain;
        this.hashCode = hashCode;
        this.sourceMaterial = sourceMaterial;
    }

    @Nullable
    String getCacheDomain() {
        return cacheDomain;
    }

    String getHashCode() {
        return hashCode;
    }

    String getSourceMaterial() {
        return sourceMaterial;
    }

    Path asPath(@Nullable String extension) {
        String filename = hashCode;
        if (extension != null) {
            filename += "." + extension;
        }

        return Paths.get(cacheDomain != null ? cacheDomain : CACHE_DOMAIN_ALL, filename);
    }
}
