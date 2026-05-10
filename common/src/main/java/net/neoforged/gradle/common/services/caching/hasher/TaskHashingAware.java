package net.neoforged.gradle.common.services.caching.hasher;

import org.gradle.api.tasks.Internal;

import java.util.Map;

/**
 * Defines a task which is hashing aware for our task hasher.
 */
public interface TaskHashingAware
{

    /**
     * Return the map of the keys and values of the properties the hasher should consider.
     *
     * @return The hashable properties.
     */
    @Internal
    Map<String, Object> getHashableProperties();
}
