package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.annotations.Nullable

/**
 * A cacheable implementation of a minecraft version reference.
 */
@CompileStatic
final class CacheableMinecraftVersion implements Serializable {

    static CacheableMinecraftVersion from(String version, Project project) {
        try {
            final MinecraftArtifactCache cache = project.getExtensions().getByType(MinecraftArtifactCache.class);
            final MinecraftVersionAndUrl resolvedVersion = cache.resolveVersion(version);

            return new CacheableMinecraftVersion(resolvedVersion.getVersion());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown version: " + version, e);
        }
    }

    private final String full;

    private CacheableMinecraftVersion(String full) {
        this.full = full;
    }

    @Input
    String getFull() {
        return full;
    }

    @Override
    String toString() {
        return this.full;
    }

    @Override
    int hashCode() {
        return this.full.hashCode();
    }

    @Override
    boolean equals(Object o) {
        if (!(o instanceof CacheableMinecraftVersion))
            return false;
        return this.full == ((CacheableMinecraftVersion) o).full;
    }
}
