package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic;

@CompileStatic
final class NamingConstants {

    private NamingConstants() {
        throw new IllegalStateException("Can not instantiate an instance of: NamingConstants. This is a utility class");
    }

    static class Extension {
        public static final String EVALUATION = "evaluation";
    }

    static class Version {

        public static final String VERSION = "version";
        public static final String MINECRAFT_VERSION = "minecraft";
    }

    static class Task {

        public static final String CACHE_LAUNCHER_METADATA = "cacheLauncherMetadata";
        public static final String CACHE_VERSION_MANIFEST = "cacheVersionManifest";
        public static final String CACHE_VERSION_ARTIFACT_CLIENT = "cacheVersionArtifactClient";
        public static final String CACHE_VERSION_ARTIFACT_SERVER = "cacheVersionArtifactServer";
        public static final String CACHE_VERSION_EXTRACTED_BUNDLE = "cacheVersionExtractedServerBundle";
        public static final String CACHE_VERSION_MAPPINGS_CLIENT = "cacheVersionMappingsClient";
        public static final String CACHE_VERSION_MAPPINGS_SERVER = "cacheVersionMappingsServer";
    }
}
